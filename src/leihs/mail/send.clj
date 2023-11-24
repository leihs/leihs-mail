(ns leihs.mail.send
  (:refer-clojure :exclude [run!])
  (:require
   [camel-snake-kebab.core :as csk]
   [clojure.java.jdbc :as jdbc]
   [clojure.tools.logging :as log]
   [leihs.mail.send.emails :as emails]
   [leihs.mail.settings :as settings]))

(def send-loop-thread (atom nil))

(defn send-loop
  []
  (.setUncaughtExceptionHandler
   (Thread/currentThread)
   (reify
     Thread$UncaughtExceptionHandler
     (uncaughtException [_ thread ex]
       (log/error ex
                  "Uncaught exception on send loop thread: "
                  (.getName thread))
       (System/exit 1))))
  (while (= (Thread/currentThread) @send-loop-thread)
    (Thread/sleep (* @settings/pause-seconds* 1000))
    (emails/send!)))

(defn run!
  []
  (reset! send-loop-thread (Thread. send-loop))
  (log/info "starting new send loop thread")
  (.start @send-loop-thread))
