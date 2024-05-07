(ns leihs.mail.send.emails
  (:require
   [clojure.set :refer [rename-keys]]
   [clojure.tools.logging :as log]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.db :refer [get-ds]]
   [leihs.core.ring-exception :as exception]
   [leihs.mail.settings :as settings]
   [logbug.catcher :as catcher]
   [logbug.thrown :as thrown]
   [next.jdbc :as jdbc :refer [execute!] :rename {execute! jdbc-execute!}]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
   [postal.core :as postal]
   [taoensso.timbre :refer [debug info warn error spy]]))

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
  [email]
  (-> (sql/update :emails)
      (sql/set email)
      (sql/where [:= :id (:id email)])
      sql-format
      (->> (jdbc-execute! (get-ds)))))

(defn- prepare-email-row
  [email result]
  (-> email
      (merge result {:updated_at [:now]})
      (update :error name)
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

(defn- send-email! [email]
  (let [prepared-email (prepare-email-message email)]
    (if (settings/smtp-enabled)
      (do (log/debug (str "sending email to: " (:email email)))
          (postal/send-message (send-message-opts) prepared-email))
      (do (log/warn "SMTP disabled. Message would be sent to: " (:email email))
          {:code 1
           :error :SMTP_DISABLED
           :message "Message not sent because of disabled SMTP setting."}))))

(defn- send-emails!
  [emails]
  (catcher/snatch
   {:level :warn}
   (doseq [email emails]
     (let [result (try (send-email! email)
                       (catch Exception e
                         (log/warn (-> e
                                       exception/get-cause
                                       thrown/to-string))
                         {:code 99,
                          :error (-> e
                                     .getClass
                                     .getName),
                          :message (.getMessage e)}))]
       (-> email
           (prepare-email-row result)
           update-email!)))))

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
