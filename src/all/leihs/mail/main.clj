(ns leihs.mail.main
  (:gen-class)
  (:refer-clojure :exclude [str keyword])
  (:require [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :as log]
            [leihs.core
             [core :refer [keyword]]
             [ds :as ds]
             [shutdown :as shutdown]]
            [leihs.mail
             [cli :as cli]
             [send :as send]
             [status :as status]]
            [logbug.catcher :as catcher]))

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
                  (send/run! options)
                  nil))

(defn -main
  [& args]
  (let [{:keys [options arguments summary]} (cli/parse args)]
    (letfn [(print-main-usage-summary
              (println (main-usage summary {:args args, :options options})))]
      (if (:help options)
        (print-main-usage-summary)
        (case (-> arguments
                  first
                  keyword)
          :run (run options)
          (println (print-main-usage-summary)))))))
