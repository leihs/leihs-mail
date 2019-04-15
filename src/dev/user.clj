(ns user
  (:require clojure.repl
            [clojure.tools.namespace.repl :as ctnr]
            leihs.mail.main
            leihs.mail.send))

(defn stop [] (leihs.mail.send/interrupt-old!))

(defn run [] (leihs.mail.main/-main "run"))
