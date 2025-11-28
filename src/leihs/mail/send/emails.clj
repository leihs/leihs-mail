(ns leihs.mail.send.emails
  (:require
   [clojure.set :refer [rename-keys]]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.db :refer [get-ds]]
   [leihs.core.ring-exception :as exception]
   [leihs.mail.send.ms365 :as ms365]
   [leihs.mail.settings :as settings]
   [logbug.catcher :as catcher]
   [logbug.thrown :as thrown]
   [next.jdbc :as jdbc :refer [execute!] :rename {execute! jdbc-execute!}]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
   [postal.core :as postal]
   [taoensso.timbre :as log]))

(def email-base-sqlmap
  (-> (sql/select :emails.*)
      (sql/from :emails)))

(defn- get-new-emails
  []
  (-> email-base-sqlmap
      (sql/where [:= :emails.trials 0])
      sql-format
      (->> (jdbc-query (get-ds)))))

(defn- update-email!
  [tx email]
  (-> (sql/update :emails)
      (sql/set email)
      (sql/where [:= :id (:id email)])
      sql-format
      (->> (jdbc-execute! tx))))

(defn- prepare-email-row
  [email result]
  (-> email
      (merge result {:updated_at [:now]})
      (update :error #(when % (name %)))
      (update :trials inc)
      (dissoc :email)))

(defn- prepare-email-message
  [email]
  (let [sender-address (settings/smtp-sender-address)]
    (-> email
        (select-keys [:from_address :to_address :subject :body])
        (rename-keys {:from_address :from, :to_address :to})
        (cond-> sender-address (assoc :sender sender-address)))))

(defn send-message-opts
  []
  (cond-> {:host (settings/smtp-address),
           :port (settings/smtp-port),
           :user (settings/smtp-username),
           :pass (settings/smtp-password),
           :localhost (settings/smtp-domain)}
    (settings/smtp-enable-starttls-auto)
    (merge {:starttls.required true, :starttls.enable true})))

(defn- send-email! [tx email]
  (let [prepared-email (prepare-email-message email)]
    (if-not (settings/smtp-enabled)
      (do (log/warn "Email sending disabled. Message would be sent to: " (:to_address email))
          {:code 1
           :error :EMAIL_DISABLED
           :message "Message not sent because email sending is disabled."})

      ;; Email sending is enabled
      (if (settings/ms365-enabled)
        (let [from-address (:from_address email)
              mailbox (ms365/get-mailbox tx from-address)]
          (if mailbox
            (let [;; Check if token needs refresh
                  token-expired? (ms365/token-expired? (:token_expires_at mailbox))
                  ;; Refresh token if needed
                  current-token (if token-expired?
                                  (when-let [new-token-data (ms365/refresh-access-token (:refresh_token mailbox))]
                                    (ms365/update-mailbox-token! tx from-address new-token-data)
                                    (:access_token new-token-data))
                                  (:access_token mailbox))]
              (if current-token
                (ms365/send-via-graph-api prepared-email current-token)
                {:code 1
                 :error :MS365_TOKEN_REFRESH_FAILED
                 :message (str "Failed to refresh MS365 token for sender: " from-address)}))
            (do (log/error (str "MS365 enabled but no mailbox configured for: " from-address))
                {:code 1
                 :error :MS365_MAILBOX_NOT_FOUND
                 :message (str "No MS365 mailbox configured for sender: " from-address)})))

        (postal/send-message (send-message-opts) prepared-email)))))

(defn- send-emails!
  [emails]
  (catcher/snatch
   {:level :warn}
   (doseq [email emails]
     (jdbc/with-transaction+options [tx (get-ds)]
       (try
         (let [result (send-email! tx email)]
           (-> email
               (prepare-email-row result)
               (->> (update-email! tx))))
         (catch Exception e
           (log/warn (-> e
                         exception/get-cause
                         thrown/to-string))
           (-> email
               (prepare-email-row {:code 99
                                   :error (-> e .getClass .getName)
                                   :message (.getMessage e)})
               (->> (update-email! tx)))))))))

(defn- send-new-emails!
  []
  (send-emails! (get-new-emails)))

(defn- get-failed-emails
  []
  (-> email-base-sqlmap
      (sql/with [:retries
                 (sql/select [[:cast
                               [:array @settings/retries-seconds*]
                               [:raw "integer[]"]]
                              :value])])
      (sql/where [:> :emails.code 0])
      (sql/where [:<=
                  :emails.trials
                  [:array_length
                   (-> (sql/select :value)
                       (sql/from :retries))
                   [:cast 1 :integer]]])
      (sql/where
       [:>
        [:extract [:raw "second from (now() - emails.updated_at)"]]
        (-> (sql/select [[:raw "value[emails.trials]"]])
            (sql/from :retries))])
      sql-format
      (->> (jdbc-query (get-ds)))))

(defn- retry-failed-emails!
  []
  (send-emails! (get-failed-emails)))

(defn send!
  []
  (send-new-emails!)
  (retry-failed-emails!))
