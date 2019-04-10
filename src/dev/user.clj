(ns user
  (:require clojure.repl
            [clojure.tools.namespace.repl :refer [refresh]]
            [leihs.mail.main :refer [-main]]))

(defn run []
  (-main "run"))

(defn reset []
  (when-let [ex (refresh :after 'user/run)] 
    (clojure.repl/pst ex)))
