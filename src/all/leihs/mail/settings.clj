(ns leihs.mail.settings
  (:require [clojure.tools.logging :as log]
            [camel-snake-kebab.core :as csk]
            [clojure.java.jdbc :as jdbc]
            [leihs.core
             [core :refer [presence]]
             [ds :refer [get-ds]]
             [sql :as sql]]
            [leihs.mail.cli :refer [cli-options]]))

(def smtp-address (atom nil))
(def smtp-port (atom nil))
(def smtp-domain (atom nil))
(def send-frequency-in-seconds (atom nil))
(def retry-frequency-in-seconds (atom nil))
(def maximum-trials (atom nil))

(def settings (atom nil))
(def smtp-settings (atom nil))

(comment (->> (filter #(->> %
                            first
                            name
                            (re-seq #"smtp_.*"))
                @settings)
              (into {})))

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

(defn reset-smtp-domain
  [options]
  (as-> :smtp-domain <>
    (option-or-setting-or-default <> options nil)
    (reset! smtp-domain <>)))

(defn reset-smtp-settings
  []
  (reset! smtp-settings
          (->> (filter #(->> %
                             first
                             name
                             (re-seq #"smtp_.*"))
                 @settings)
               (into {}))))

(defn init
  [options]
  (reset! settings
          (-> (sql/select :*)
              (sql/from :settings)
              sql/format
              (->> (jdbc/query (get-ds)))
              first))
  (reset-smtp-settings)
  (reset-smtp-address options)
  (reset-smtp-port options)
  (reset-smtp-domain options)
  (->> options
       :send-frequency-in-seconds
       (reset! send-frequency-in-seconds))
  (->> options
       :retry-frequency-in-seconds
       (reset! retry-frequency-in-seconds))
  (->> options
       :maximum-trials
       (reset! maximum-trials)))
