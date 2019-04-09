(ns user
  (:require [clojure.tools.logging :as log]
            [clojure.tools.namespace.repl :refer [refresh]]
            [leihs.mail.main :refer [-main]]
            clojure.repl))

(defn run []
  (-main "run"))

(defn reset []
  (when-let [ex (refresh :after 'app/run)] 
    (clojure.repl/pst ex)))
