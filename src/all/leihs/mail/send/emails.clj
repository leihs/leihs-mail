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

(def ^:private email-base-sqlmap
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
  (-> email
      (select-keys [:sender :email :subject :body])
      (rename-keys {:sender :from, :email :to})))

(defn- send-emails!
  [emails]
  (catcher/snatch
    {:level :warn}
    (doseq [email emails]
      (log/debug (str "sending email to: " (:email email)))
      (let [result (try (->> email
                             prepare-email-message
                             (postal/send-message {:host @settings/smtp-address,
                                                   :port @settings/smtp-port}))
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
            (prepare-email-row (log/spy result))
            update-email!)))))

(defn- send-new-emails! [] (send-emails! (get-new-emails)))

(defn- get-failed-emails
  []
  (-> email-base-sqlmap
      (sql/merge-where [:> :emails.code 0])
      (sql/merge-where
        [:>
         (sql/call :extract (sql/raw "second from (now() - emails.updated_at)"))
         @settings/retry-frequency-in-seconds])
      (sql/merge-where [:< :emails.trials @settings/maximum-trials])
      sql/format
      (->> (jdbc/query (get-ds)))))

(defn- retry-failed-emails! [] (send-emails! (get-failed-emails)))

(defn send! [] (send-new-emails!) (retry-failed-emails!))

(comment
  (-> email-base-sqlmap
      (sql/merge-where [:> :emails.code 0])
      (sql/merge-where
        [:>
         (sql/call :extract (sql/raw "minute from (now() - emails.updated_at)"))
         30])
      (sql/merge-where [:< :emails.trials 2])
      sql/format)
  (postal/send-message {:host "localhost"}
                       {:from "bar@example.com",
                        :to ["foo@example.com"],
                        :subject "Test.",
                        :body "Test.",
                        :X-Tra "Something else"}))
