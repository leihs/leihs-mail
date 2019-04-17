(ns leihs.mail.settings
  (:require [camel-snake-kebab.core :as csk]
            [clojure.java.jdbc :as jdbc]
            [leihs.core
             [ds :refer [get-ds]]
             [sql :as sql]]
            [leihs.mail.cli :refer [defaults]]))

(def smtp-address (atom nil))
(def smtp-port (atom nil))
(def send-frequency-in-seconds (atom nil))
(def retry-frequency-in-seconds (atom nil))
(def maximum-trials (atom nil))

(def settings (atom nil))

(defn- option-or-setting-or-default
  [kw options default]
  (or (kw options)
      (->> kw
           name
           csk/->snake_case
           (get @settings))
      default))

(defn reset-smtp-address
  [options]
  (as-> :smtp-address <>
    (option-or-setting-or-default <> options "localhost")
    (reset! smtp-address <>)))

(defn reset-smtp-port
  [options]
  (as-> :smtp-port <>
    (option-or-setting-or-default <> options 25)
    (reset! smtp-port <>)))

(defn init
  [options]
  (reset! settings
          (-> (sql/select :*)
              (sql/from :settings)
              sql/format
              (->> (jdbc/query (get-ds)))))
  (reset-smtp-address options)
  (reset-smtp-port options)
  (->> options
       :send-frequency-in-seconds
       (reset! send-frequency-in-seconds))
  (->> options
       :retry-frequency-in-seconds
       (reset! retry-frequency-in-seconds))
  (->> options
       :maximum-trials
       (reset! maximum-trials)))
