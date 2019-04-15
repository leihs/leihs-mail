(ns leihs.mail.main
  (:gen-class)
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [str keyword presence]]
            [clojure.tools.logging :as log]
            [logbug.catcher :as catcher]
            [clojure.tools.cli :as cli :refer [parse-opts]]
            [leihs.core.ds :as ds]
            [leihs.mail.status :as status]
            [leihs.mail.send :as send]
            [leihs.core.url.jdbc :as jdbc-url]
            [clojure.pprint :refer [pprint]]
            [leihs.core.shutdown :as shutdown]))

(def defaults
  {:LEIHS_DATABASE_URL
     "jdbc:postgresql://leihs:leihs@localhost:5432/leihs?min-pool-size=1&max-pool-size=5"})

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
         extend-pg-params)]])

(defn- main-usage
  [options-summary & more]
  (->>
    ["Leihs Mail"
     ""
     "usage: leihs-mail [<opts>] SCOPE [<scope-opts>] [<args>]"
     ""
     "Options:"
     options-summary
     ""
     ""
     (when more
       ["-------------------------------------------------------------------"
        (with-out-str (pprint more))
        "-------------------------------------------------------------------"])]
    flatten
    (clojure.string/join \newline)))

(defn- run
  [options]
  (catcher/snatch {:return-fn (fn [e] (System/exit -1))}
                  (log/info "Invoking run with options: " options)
                  (shutdown/init options)
                  (let [status (status/init)]
                    (ds/init (:database-url options)
                             (:health-check-registry status)))
                  (send/run!)
                  nil))

(defn -main
  [& args]
  (let [{:keys [options arguments summary]}
          (cli/parse-opts args cli-options :in-order true)]
    (cond (:help options) (println (main-usage summary
                                               {:args args, :options options}))
          :else (case (-> arguments
                          first
                          keyword)
                  :run (run options)
                  (println (main-usage summary
                                       {:args args, :options options}))))))
