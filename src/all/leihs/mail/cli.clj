(ns leihs.mail.cli
  (:refer-clojure :exclude [str keyword])
  (:require [clojure.tools.logging :as log]
            [clojure.tools.cli :as cli]
            [clojure.spec.alpha :as spec]
            [clojure.repl :refer [doc]]
            [leihs.core.core :refer [presence keyword str]]
            [leihs.core.url.jdbc :as jdbc-url]))

(def defaults
  {:LEIHS_DATABASE_URL
     "jdbc:postgresql://leihs:leihs@localhost:5432/leihs?min-pool-size=1&max-pool-size=5",
   :LEIHS_MAIL_SEND_FREQUENCY_IN_SECONDS "5",
   :LEIHS_MAIL_RETRY_FREQUENCY_IN_SECONDS "10",
   :LEIHS_MAIL_MAXIMUM_TRIALS "2",
   :LEIHS_MAIL_SMTP_ADDRESS "localhost",
   :LEIHS_MAIL_SMTP_PORT "25"})

(defn- get-from-env
  [kw]
  (-> (System/getenv)
      (get (str kw) nil)
      presence))

(defn env-or-default [kw] (or (get-from-env kw) (get defaults kw nil)))

(defn extend-pg-params
  [params]
  (assoc params
    :password (or (:password params) (System/getenv "PGPASSWORD"))
    :username (or (:username params) (System/getenv "PGUSER"))
    :port (or (:port params) (System/getenv "PGPORT"))))

(spec/def ::database-url-val string?)
(spec/def ::send-frequency-in-seconds-val integer?)
(spec/def ::retry-frequency-in-seconds-val integer?)
(spec/def ::maximum-trials-val integer?)
(spec/def ::smtp-address-val (spec/or :nil nil? :string string?))
(spec/def ::smtp-port-val (spec/or :nil nil? :string integer?))

(comment
  (spec/assert ::send-frequency-in-seconds-val 10)
  (spec/valid? ::database-url-val "foo"))

(def cli-options
  [["-h" "--help"]
   ["-d" "--database-url LEIHS_DATABASE_URL"
    (str "default: " (:LEIHS_DATABASE_URL defaults))
    :default
    (->> :LEIHS_DATABASE_URL
         env-or-default
         (spec/assert ::database-url-val)
         jdbc-url/dissect
         extend-pg-params)
    :parse-fn
    #(-> %
         jdbc-url/dissect
         extend-pg-params)]
   [nil "--send-frequency-in-seconds LEIHS_MAIL_SEND_FREQUENCY_IN_SECONDS"
    (str "default: " (:LEIHS_MAIL_SEND_FREQUENCY_IN_SECONDS defaults))
    :default
    (->> :LEIHS_MAIL_SEND_FREQUENCY_IN_SECONDS
         env-or-default
         Integer/parseInt
         (spec/assert ::send-frequency-in-seconds-val))
    :parse-fn #(Integer/parseInt %)]
   [nil "--retry-frequency-in-seconds LEIHS_MAIL_RETRY_FREQUENCY_IN_SECONDS"
    (str "default: " (:LEIHS_MAIL_RETRY_FREQUENCY_IN_SECONDS defaults))
    :default
    (->> :LEIHS_MAIL_RETRY_FREQUENCY_IN_SECONDS
         env-or-default
         Integer/parseInt
         (spec/assert ::retry-frequency-in-seconds-val))
    :parse-fn #(Integer/parseInt %)]
   [nil "--maximum-trials LEIHS_MAIL_MAXIMUM_TRIALS"
    (str "default: " (:LEIHS_MAIL_MAXIMUM_TRIALS defaults))
    :default
    (->> :LEIHS_MAIL_MAXIMUM_TRIALS
         env-or-default
         Integer/parseInt
         (spec/assert ::maximum-trials-val))
    :parse-fn #(Integer/parseInt %)]
   [nil "--smtp-address LEIHS_MAIL_SMTP_ADDRESS"
    "default: settings.smtp_address or localhost"
    :default
    (->> :LEIHS_MAIL_SMTP_ADDRESS
         get-from-env
         (spec/assert ::smtp-address-val))]
   [nil "--smtp-port LEIHS_MAIL_SMTP_PORT"
    "default: settings.smtp_port or 25"
    :default
    (spec/assert ::smtp-port-val
                 (some-> :LEIHS_MAIL_SMTP_PORT
                         get-from-env
                         Integer/parseInt))
    :parse-fn #(Integer/parseInt %)]])

(defn parse [args] (cli/parse-opts args cli-options :in-order true))
