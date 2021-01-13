(ns app
  (:require clojure.repl
            user
            [clojure.tools.namespace.repl :as ctnr]))

(ctnr/disable-reload!)

(defn reset
  []
  (if-let [ex (ctnr/refresh :after 'user/run)] (clojure.repl/pst ex)))
