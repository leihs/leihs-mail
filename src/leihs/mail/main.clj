(ns leihs.mail.main
  (:require
   [clj-yaml.core :as yaml]
   [clojure.pprint :refer [pprint]]
   [clojure.tools.cli :as cli :refer [parse-opts]]
   [environ.core :refer [env]]
   [leihs.core.logging]
   [leihs.core.repl :as repl]
   [leihs.mail.run :as run]
   [logbug.catcher :as catcher]
   [logbug.debug :as debug]
   [logbug.thrown :as thrown]
   [taoensso.timbre :refer [debug info warn error]])
  (:gen-class))

(thrown/reset-ns-filter-regex #"^(leihs|cider)\..*")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def cli-options
  (concat
   [["-h" "--help"]
    [nil "--dev-mode DEV_MODE" "dev mode"
     :default (or (some-> :dev-mode env yaml/parse-string) false)
     :parse-fn #(yaml/parse-string %)
     :validate [boolean? "Must parse to a boolean"]]]
   repl/cli-options))

(defn main-usage [options-summary & more]
  (->> ["Leihs Mail"
        ""
        "usage: leihs-mail [<opts>] SCOPE/CMD [<scope-opts>] [<args>]"
        ""
        "Options:"
        options-summary
        ""
        ""
        (when more
          ["-------------------------------------------------------------------"
           (with-out-str (pprint more))
           "-------------------------------------------------------------------"])]
       flatten (clojure.string/join \newline)))

(defonce args* (atom nil))

(defn main []
  (leihs.core.logging/init)
  (info 'main @args*)
  (let [args @args*
        {:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options :in-order true)
        options (into (sorted-map) options)
        cmd (some-> arguments first keyword)]
    (info {'options options 'cmd cmd})
    (repl/init options)
    (cond
      (:help options) (println (main-usage summary {:args args :options options}))
      :else (case cmd
              :run (run/main options (rest arguments))
              (println (main-usage summary {:args args :options options}))))))

; dynamic restart on require
(when @args* (main))

(defn -main [& args]
  (reset! args* args)
  (main))

;(-main "-h")
;(-main "run")
