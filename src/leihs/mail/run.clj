(ns leihs.mail.run
  (:refer-clojure :exclude [str keyword])
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [clojure.tools.cli :as cli]
   [clojure.tools.logging :as logging]
   [leihs.core.db :as db]
   [leihs.core.shutdown :as shutdown]
   [leihs.core.status :as status]
   [leihs.core.url.jdbc]
   [leihs.mail.send]
   [leihs.mail.settings :as settings]
   [logbug.catcher :as catcher]))

(defn run [options]
  (catcher/snatch
   {:return-fn (fn [_] (System/exit -1))}
   (logging/info "Invoking run with options: " options)
   (shutdown/init options)
   (let [status (status/init)]
     (db/init options (:health-check-registry status)))
   (settings/init options)
   (leihs.mail.send/run!)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def cli-options
  (concat
   [["-h" "--help"]
    shutdown/pid-file-option]
   db/cli-options
   settings/cli-opts))

(defn main-usage [options-summary & more]
  (->> ["Leihs-Mail run"
        ""
        "usage: leihs-mail [<opts>] run [<run-opts>] "
        ""
        "Options:"
        options-summary
        ""
        ""
        (when more
          ["-------------------------------------------------------------------"
           (with-out-str (pprint more))
           "-------------------------------------------------------------------"])]
       flatten (str/join \newline)))

(defn main [gopts args]
  (let [{:keys [options summary]}
        (cli/parse-opts args cli-options :in-order true)
        options (merge gopts options)]
    (cond
      (:help options) (println (main-usage summary {:args args :options options}))
      :else (run options))))
