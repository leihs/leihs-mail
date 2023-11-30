(ns leihs.mail.settings
  (:require
   [clj-yaml.core :as yaml]
   [clojure.core.memoize :as memoize]
   [cuerdas.core :as string :refer [kebab snake upper]]
   [environ.core :refer [env]]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core
    [core :refer [presence]]
    [db :refer [get-ds-next]]]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
   [taoensso.timbre :refer [info]]))

;;; state ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def pause-seconds* (atom nil))
(def retries-seconds* (atom nil))

;;; cli ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn long-opt-for-key [k]
  (str "--" (kebab k) " " (-> k snake upper)))

(def mail-send-pause-secs-key :mail-send-pause-secs)
(def send-pause-secs-opt
  [nil (long-opt-for-key mail-send-pause-secs-key)
   :default (-> (or (some-> mail-send-pause-secs-key env presence)
                    "1")
                Integer/parseInt)
   :parse-fn #(Integer/parseInt %)
   :validate [#(and (< 0 % 1000) (int? %))
              #(str "must be an int between 0 and 100")]])

(def mail-retries-seconds*-key :mail-retries-seconds)
(def retries-in-seconds-opt
  [nil (long-opt-for-key mail-retries-seconds*-key)
   :default (or (some-> retries-in-seconds-opt
                        env presence yaml/parse-string)
                [5,10,30,60,300,3600,18000])
   :parse-fn yaml/parse-string
   :validate [#(and (seq %)
                    (every? int? %))
              #("YAML parseable, non empty array of integers")]])

(def cli-opts
  [send-pause-secs-opt
   retries-in-seconds-opt])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn db-settings-uncached []
  (-> (sql/select :*)
      (sql/from :smtp_settings)
      sql-format
      (->> (jdbc-query (get-ds-next)))
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

(defn smtp-enabled []
  (:enabled (db-settings)))

(defn all []
  {:smtp-address (smtp-address)
   :smtp-port (smtp-port)
   :smtp-username (smtp-username)
   :smtp-password (smtp-password)
   :smtp-domain (smtp-domain)
   :smtp-sender-address (smtp-sender-address)
   :smtp-enable-starttls-auto (smtp-enable-starttls-auto)
   :smtp-enabled (smtp-enabled)
   :pause-seconds* @pause-seconds*
   :retries-seconds* @retries-seconds*})

;;; init ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn init [opts]
  (info "initializing settings ...")
  (reset! pause-seconds*
          (get opts mail-send-pause-secs-key))
  (reset! retries-seconds*
          (get opts mail-retries-seconds*-key))
  (info "initialized settings " {'pause-seconds* pause-seconds*
                                 'retries-seconds* retries-seconds*}))
