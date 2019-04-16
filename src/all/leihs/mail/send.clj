(ns leihs.mail.send
  (:refer-clojure :exclude [run!])
  (:require [clojure.tools.logging :as log]
            [leihs.mail.send.emails :as emails]
            [leihs.mail.cli :as cli]))

(def send-loop-thread (atom nil))

(def smtp-address (atom nil))
(def smtp-port (atom nil))

(defn- setting-or-option
  [kw]
  (-> kw
      name
      camelize))

(defn- send-loop
  []
  (while (not (.isInterrupted (Thread/currentThread)))
    (Thread/sleep (-> @cli/options
                      :send-frequency-in-seconds
                      (* 1000)))
    (emails/send!)))

(defn interrupt-old!
  []
  (when @send-loop-thread
    (log/info "interrupting send loop thread")
    (.interrupt @send-loop-thread))
  (reset! send-loop-thread nil))

(defn- start-new!
  []
  (reset! send-loop-thread (Thread. send-loop))
  (log/info "starting new send loop thread")
  (.start @send-loop-thread))

(defn- stop-old!
  []
  (log/info "stoping send loop thread")
  (.stop @send-loop-thread)
  (reset! send-loop-thread nil))

(defn run!
  [options]
  (interrupt-old!)
  (reset! smtp-address (:smtp-address options))
  (reset! smtp-port (:smtp-port options))
  (start-new!))

(comment
  (run!)
  (interrupt-old!)
  (stop-old!)
  (.isAlive @send-loop-thread))
