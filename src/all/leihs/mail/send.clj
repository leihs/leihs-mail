(ns leihs.mail.send
  (:require [clojure.tools.logging :as log]))

(def send-loop-thread (atom nil))

(defn send-loop
  []
  (while (not (.isInterrupted @send-loop-thread))
         (Thread/sleep 100)
         (log/debug "foo")))

(defn interrupt [] (.interrupt @send-loop-thread))

(defn stop [] (.stop @send-loop-thread))

(defn run
  []
  (let [t (Thread. send-loop)]
    (reset! send-loop-thread t)
    (.start @send-loop-thread)))

(comment (run) (interrupt) (stop) (.isAlive @send-loop-thread))
