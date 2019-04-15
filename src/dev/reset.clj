(ns reset
  (:require clojure.repl
            [clojure.tools.namespace.repl :as ctnr]))

(ctnr/disable-reload!)

(defn stop
  []
  "As this namespace is excluded from reloading we have do resolve vars
  at runtime, in order no to keep stale references."
  (some-> (find-ns 'user)
          (ns-resolve 'stop)
          (apply [])))

(defn reset
  []
  (stop)
  (if-let [ex (ctnr/refresh :after 'user/run)] (clojure.repl/pst ex)))
