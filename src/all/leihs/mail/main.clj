(ns leihs.mail.main
  (:gen-class)
  (:require [clojure.tools.logging :as log]))

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]}
          (cli/parse-opts args cli-options :in-order true)
        pass-on-args (->> [options (rest arguments)]
                          flatten
                          (into []))]
    (cond (:help options) (println (main-usage summary
                                               {:args args, :options options}))
          :else (case (-> arguments
                          first
                          keyword)
                  :run (apply run/-main (rest arguments))
                  (println (main-usage summary
                                       {:args args, :options options}))))))
