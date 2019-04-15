(ns leihs.mail.send.emails
  (:require [postal.core :as postal]
            [leihs.core.ds :refer [get-ds]]
            [leihs.core.sql :as sql]
            [clojure.java.jdbc :as jdbc]
            [clojure.set :refer [rename-keys]]
            [logbug.catcher :as catcher]
            [clojure.tools.logging :as log]
            [leihs.mail.constants :as constants]))

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

(defn- send-new-emails!
  []
  (catcher/snatch {:level :warn}
                  (doseq [email (get-new-emails)]
                    (log/debug (str "sending email to: " (:email email)))
                    (let [result (-> email
                                     prepare-email-message
                                     postal/send-message)]
                      (-> email
                          (prepare-email-row result)
                          update-email!)))))

(defn- get-failed-emails
  []
  (-> email-base-sqlmap
      (sql/merge-where [:> :emails.code 0])
      (sql/merge-where
        [:>
         (sql/call :extract (sql/raw "minute from (now() - emails.updated_at)"))
         constants/retry-frequency-in-minutes])
      (sql/merge-where [:< :emails.trials constants/maximum-trials])
      sql/format
      (->> (jdbc/query (get-ds)))))

(defn- retry-failed-emails!
  []
  (catcher/snatch {:level :warn}
                  (doseq [email (get-failed-emails)]
                    (log/debug (str "sending email to: " (:email email)))
                    (let [result (-> email
                                     prepare-email-message
                                     postal/send-message)]
                      (-> email
                          (prepare-email-row result)
                          update-email!)))))

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
  (-> (sql/insert-into :emails)
      (sql/values [{(sql/quote-identifier :to) "foo@examples.com"}])
      sql/format
      ; (->> (jdbc/execute! (get-ds)))
    )
  (postal/send-message ;{:host (:smtp_address settings)}
                       {:from "me@nitaai.com",
                        :to ["foo@example.com"],
                        :subject "matus local testing",
                        :body "Test.",
                        :X-Tra "Something else"}))
