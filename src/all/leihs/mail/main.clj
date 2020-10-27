(ns leihs.mail.main
  (:gen-class)
  (:refer-clojure :exclude [str keyword])
  (:require [clj-pid.core :as pid]
            [clojure.pprint :refer [pprint]]
            [clojure.spec.alpha :as spec]
            [clojure.tools.logging :as log]
            [leihs.core
             [core :refer [keyword str]]
             [ds :as ds]
             [shutdown :as shutdown]]
            [leihs.mail
             [cli :as cli]
             [send :as send]
             [settings :as settings]
             [status :as status]]
            [logbug.catcher :as catcher]
            [signal.handler]
            ))

(spec/check-asserts true)

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

(spec/def ::database-url map?)
(spec/def ::send-frequency-in-seconds integer?)
(spec/def ::retries-in-seconds (spec/coll-of integer?))
(spec/def ::smtp-address string?)
(spec/def ::smtp-port integer?)
(spec/def ::smtp-domain (spec/or :nil nil? :string string?))

(spec/def ::options
  (spec/keys :req-un
             [::database-url
              ::send-frequency-in-seconds
              ::retries-in-seconds
              ::smtp-address
              ::smtp-port
              ::smtp-domain]))

(defn- run
  [options]
  (catcher/snatch {:return-fn (fn [e] (System/exit -1))}
                  (settings/init options)
                  (shutdown/init options)
                  (let [status (status/init)]
                    (ds/init (:database-url options)
                             (:health-check-registry status)))
                  (send/run!)
                  (log/info "Send loop running with the following configuration: "
                            (settings/all))
                  nil))

(defn -main
  [& args]
  (let [{:keys [options summary]} (cli/parse (rest args))]
    (letfn [(print-main-usage-summary
              []
              (println (main-usage summary {:args args, :options options})))]
      (if (:help options)
        (print-main-usage-summary)
        (case (-> args
                  first
                  keyword)
          :run (run options)
          (println (print-main-usage-summary)))))))
