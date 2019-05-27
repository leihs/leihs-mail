(ns leihs.mail.settings
  (:require [clojure.tools.logging :as log]
            [camel-snake-kebab.core :as csk]
            [clojure.java.jdbc :as jdbc]
            [leihs.core
             [core :refer [presence]]
             [ds :refer [get-ds]]
             [sql :as sql]]))

(def options (atom nil))
(def send-frequency-in-seconds (atom nil))
(def retries-in-seconds (atom nil))

(defn settings
  []
  (-> (sql/select :*)
      (sql/from :settings)
      sql/format
      (->> (jdbc/query (get-ds)))
      first))

(defn- option-or-setting-or-default
  [kw default]
  (or (kw @options)
      (->> kw
           name
           csk/->snake_case
           keyword
           (get (settings)))
      default))

(defn smtp-address
  []
  (option-or-setting-or-default :smtp-address "localhost"))

(defn smtp-port
  []
  (option-or-setting-or-default :smtp-port 25))

(defn smtp-domain
  []
  (option-or-setting-or-default :smtp-domain nil))

(defn smtp-sender-address
  []
  (:smtp_sender_address (settings)))

(defn smtp-enable-starttls-auto
  []
  (:smtp_enable_starttls_auto (settings)))

(defn init
  [opts]
  (reset! options opts)
  (->> opts
       :send-frequency-in-seconds
       (reset! send-frequency-in-seconds))
  (->> opts
       :retries-in-seconds
       (reset! retries-in-seconds)))
