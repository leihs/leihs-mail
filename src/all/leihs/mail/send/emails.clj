(ns leihs.mail.send.emails
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.set :refer [rename-keys]]
            [clojure.tools.logging :as log]
            [leihs.core
             [ds :refer [get-ds]]
             [sql :as sql]
             [ring-exception :as exception]]
            [leihs.mail.settings :as settings]
            [logbug.catcher :as catcher]
            [logbug.thrown :as thrown]
            [postal.core :as postal]))

(def email-base-sqlmap
  (-> (sql/select :emails.* :users.email)
      (sql/from :emails)
      (sql/merge-left-join :users [:= :users.id :emails.user_id])))

(defn- get-new-emails
  []
  (-> email-base-sqlmap
      (sql/merge-where [:= :emails.trials 0])
      sql/format
      (->> (jdbc/query (get-ds)))))

(defn- update-email!
  [email]
  (-> (sql/update :emails)
      (sql/set email)
      (sql/where [:= :id (:id email)])
      sql/format
      (->> (jdbc/execute! (get-ds)))))

(defn- prepare-email-row
  [email result]
  (-> email
      (merge result {:updated_at (sql/call :now)})
      (update :error name)
      (update :trials inc)
      (dissoc :email)))

(defn- prepare-email-message
  [email]
  (let [sender-address (settings/smtp-sender-address)]
    (-> email
        (select-keys [:from_address :email :subject :body])
        (rename-keys {:from_address :from, :email :to})
        (cond-> sender-address (assoc :sender sender-address)))))

(defn host-settings
  []
  (cond-> {:host (settings/smtp-address),
           :port (settings/smtp-port),
           :user (:smtp_username (settings/settings)),
           :pass (:smtp_password (settings/settings)),
           :localhost (settings/smtp-domain)}
    (settings/smtp-enable-starttls-auto) (merge {:starttls.required true,
                                                 :starttls.enable true})))

(defn- send-emails!
  [emails]
  (catcher/snatch
    {:level :warn}
    (doseq [email emails]
      (log/debug (str "sending email to: " (:email email)))
      (let [result (try (->> email
                             prepare-email-message
                             (postal/send-message (host-settings)))
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
                 (sql/select [(sql/call :cast
                                        (sql/array @settings/retries-in-seconds)
                                        (sql/raw "integer[]"))
                              :value])])
      (sql/merge-where [:> :emails.code 0])
      (sql/merge-where [:<=
                        :emails.trials
                        (sql/call :array_length
                                  (-> (sql/select :value)
                                      (sql/from :retries))
                                  (sql/call :cast 1 :integer))])
      (sql/merge-where
        [:>
         (sql/call :extract (sql/raw "second from (now() - emails.updated_at)"))
         (-> (sql/select (sql/raw "value[emails.trials]"))
             (sql/from :retries))])
      sql/format
      (->> (jdbc/query (get-ds)))))

(defn- retry-failed-emails!
  []
  (send-emails! (get-failed-emails)))

(defn send!
  []
  (send-new-emails!)
  (retry-failed-emails!))
