(ns leihs.mail.settings
  (:require [clojure.tools.logging :as log]
            [camel-snake-kebab.core :as csk]
            [clojure.java.jdbc :as jdbc]
            [clojure.core.memoize :as memoize]
            [leihs.core
             [core :refer [presence]]
             [ds :refer [get-ds]]
             [sql :as sql]]))

(def options (atom nil))
(def send-frequency-in-seconds (atom nil))
(def retries-in-seconds (atom nil))

(defn db-settings-uncached []
  (-> (sql/select :*)
      (sql/from :smtp_settings)
      sql/format
      (->> (jdbc/query (get-ds)))
      first))

(def db-settings
  (memoize/ttl db-settings-uncached :ttl/threshold 1000))

(defn smtp-address
  []
  (:address (db-settings)))

(defn smtp-port
  []
  (:port (db-settings)))

(defn smtp-domain
  []
  (:domain (db-settings)))

(defn smtp-sender-address
  []
  (:sender_address (db-settings)))

(defn smtp-enable-starttls-auto
  []
  (:enable_starttls_auto (db-settings)))

(defn smtp-username []
  (:username (db-settings)))

(defn smtp-password []
  (:password (db-settings)))

(defn all []
  {:smtp-address (smtp-address)
   :smtp-port (smtp-port)
   :smtp-username (smtp-username)
   :smtp-password (smtp-password)
   :smtp-domain (smtp-domain)
   :smtp-sender-address (smtp-sender-address)
   :smtp-enable-starttls-auto (smtp-enable-starttls-auto)
   :send-frequency-in-seconds @send-frequency-in-seconds
   :retries-in-seconds @retries-in-seconds})

(defn init
  [opts]
  (reset! options opts)
  (->> opts
       :send-frequency-in-seconds
       (reset! send-frequency-in-seconds))
  (->> opts
       :retries-in-seconds
       (reset! retries-in-seconds)))
