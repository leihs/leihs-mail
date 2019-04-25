(ns leihs.mail.main
  (:gen-class)
  (:refer-clojure :exclude [str keyword])
  (:require [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :as log]
            [clojure.spec.alpha :as spec]
            [leihs.core
             [core :refer [str keyword]]
             [ds :as ds]
             [shutdown :as shutdown]]
            [leihs.mail
             [cli :as cli]
             [send :as send]
             [settings :as settings]
             [status :as status]]
            [clj-pid.core :as pid]
            [logbug.catcher :as catcher]))

(spec/check-asserts true)

(defn handle-pidfile
  []
  (let [pid-file "./tmp/server_pid"]
    (.mkdirs (java.io.File. "./tmp"))
    (pid/save pid-file)
    (log/info (str "pid-file written to " pid-file))
    (pid/delete-on-shutdown! pid-file)))

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
                  (shutdown/init options)
                  (let [status (status/init)]
                    (ds/init (:database-url options)
                             (:health-check-registry status)))
                  (send/run! options)
                  (log/info "Invoking run with options: "
                            (-> options
                                (assoc :smtp-address @settings/smtp-address)
                                (assoc :smtp-port @settings/smtp-port)))
                  (handle-pidfile)
                  nil))

(defn -main
  [& args]
  (let [{:keys [options arguments summary]} (cli/parse args)]
    (letfn [(print-main-usage-summary
              []
              (println (main-usage summary {:args args, :options options})))]
      (if (:help options)
        (print-main-usage-summary)
        (case (-> arguments
                  first
                  keyword)
          :run (run options)
          (println (print-main-usage-summary)))))))
