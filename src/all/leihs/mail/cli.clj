(ns leihs.mail.cli
  (:gen-class)
  (:refer-clojure :exclude [str keyword])
  (:require [clojure.tools.cli :as cli]
            [leihs.core.core :refer [presence str]]
            [leihs.core.url.jdbc :as jdbc-url]))

(def defaults
  {:LEIHS_DATABASE_URL
     "jdbc:postgresql://leihs:leihs@localhost:5432/leihs?min-pool-size=1&max-pool-size=5",
   :LEIHS_MAIL_SEND_FREQUENCY_IN_SECONDS 5,
   :LEIHS_MAIL_RETRY_FREQUENCY_IN_MINUTES 5,
   :LEIHS_MAIL_MAXIMUM_TRIALS 2})

(defn env-or-default
  [kw]
  (or (-> (System/getenv)
          (get (str kw) nil)
          presence)
      (get defaults kw nil)))

(defn extend-pg-params
  [params]
  (assoc params
    :password (or (:password params) (System/getenv "PGPASSWORD"))
    :username (or (:username params) (System/getenv "PGUSER"))
    :port (or (:port params) (System/getenv "PGPORT"))))

(def ^:private cli-options
  [["-h" "--help"]
   ["-d" "--database-url LEIHS_DATABASE_URL"
    (str "default: " (:LEIHS_DATABASE_URL defaults))
    :default
    (-> (env-or-default :LEIHS_DATABASE_URL)
        jdbc-url/dissect
        extend-pg-params)
    :parse-fn
    #(-> %
         jdbc-url/dissect
         extend-pg-params)]
   [nil "--send-frequency-in-seconds LEIHS_MAIL_SEND_FREQUENCY_IN_SECONDS"
    (str "default: " (:LEIHS_MAIL_SEND_FREQUENCY_IN_SECONDS defaults))
    :default (env-or-default :LEIHS_MAIL_SEND_FREQUENCY_IN_SECONDS)
    :parse-fn #(Integer/parseInt %)]
   [nil "--retry-frequency-in-minutes LEIHS_MAIL_RETRY_FREQUENCY_IN_MINUTES"
    (str "default: " (:LEIHS_MAIL_RETRY_FREQUENCY_IN_MINUTES defaults))
    :default (env-or-default :LEIHS_MAIL_RETRY_FREQUENCY_IN_MINUTES)
    :parse-fn #(Integer/parseInt %)]
   [nil "--maximum-trials LEIHS_MAIL_MAXIMUM_TRIALS"
    (str "default: " (:LEIHS_MAIL_MAXIMUM_TRIALS defaults))
    :default (env-or-default :LEIHS_MAIL_MAXIMUM_TRIALS)
    :parse-fn #(Integer/parseInt %)]])

(defn parse
  [args]
  (let [{:keys [options], :as result}
          (cli/parse-opts args cli-options :in-order true)]
    (defonce options options)
    result))
