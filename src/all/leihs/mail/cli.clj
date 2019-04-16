(ns leihs.mail.cli
  (:gen-class)
  (:refer-clojure :exclude [str keyword])
  (:require [clojure.tools.logging :as log]
            [clojure.tools.cli :as cli]
            [leihs.core.core :refer [presence str]]
            [leihs.core.url.jdbc :as jdbc-url]))

(def options (atom nil))

(def defaults
  {:LEIHS_DATABASE_URL
     "jdbc:postgresql://leihs:leihs@localhost:5432/leihs?min-pool-size=1&max-pool-size=5",
   :LEIHS_MAIL_SEND_FREQUENCY_IN_SECONDS 5,
   :LEIHS_MAIL_RETRY_FREQUENCY_IN_SECONDS 5,
   :LEIHS_MAIL_MAXIMUM_TRIALS 2,
   :LEIHS_MAIL_SMTP_ADDRESS "localhost",
   :LEIHS_MAIL_SMTP_PORT 25})

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
    :default
    (-> :LEIHS_MAIL_SEND_FREQUENCY_IN_SECONDS
        env-or-default
        Integer/parseInt)
    :parse-fn #(Integer/parseInt %)]
   [nil "--retry-frequency-in-seconds LEIHS_MAIL_RETRY_FREQUENCY_IN_SECONDS"
    (str "default: " (:LEIHS_MAIL_RETRY_FREQUENCY_IN_SECONDS defaults))
    :default
    (-> :LEIHS_MAIL_RETRY_FREQUENCY_IN_SECONDS
        env-or-default
        Integer/parseInt)
    :parse-fn #(Integer/parseInt %)]
   [nil "--maximum-trials LEIHS_MAIL_MAXIMUM_TRIALS"
    (str "default: " (:LEIHS_MAIL_MAXIMUM_TRIALS defaults))
    :default
    (-> :LEIHS_MAIL_MAXIMUM_TRIALS
        env-or-default
        Integer/parseInt)
    :parse-fn #(Integer/parseInt %)]
   [nil "--smtp-address LEIHS_MAIL_SMTP_ADDRESS"
    "default: settings.smtp_address or localhost"]
   [nil "--smtp-port LEIHS_MAIL_SMTP_PORT"
    "default: settings.smtp_port or 25"]])

(defn parse
  [args]
  (let [{opts :options, :as result}
          (cli/parse-opts args cli-options :in-order true)]
    (reset! options opts)
    result))
