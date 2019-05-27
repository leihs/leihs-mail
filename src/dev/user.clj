(ns user
  (:require clojure.repl
            [clojure.tools.namespace.repl :as ctnr]
            leihs.mail.main
            leihs.mail.send))

(defn run
  [& args]
  (apply leihs.mail.main/-main "run" args))
