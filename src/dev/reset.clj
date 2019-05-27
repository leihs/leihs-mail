(ns reset
  (:require clojure.repl
            [clojure.tools.namespace.repl :as ctnr]))

(ctnr/disable-reload!)

(defn reset
  []
  (if-let [ex (ctnr/refresh :after 'user/run)] (clojure.repl/pst ex)))
