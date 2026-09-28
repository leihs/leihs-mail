(ns leihs.mail.send.smtp-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [leihs.mail.send.smtp :as smtp])
  (:import
   [java.io BufferedReader InputStreamReader PrintWriter]
   [java.net ServerSocket]))

(defn- write-line [out line]
  (.print out (str line "\r\n"))
  (.flush out))

(defn- fake-smtp-server
  "A minimal SMTP server on an ephemeral port that accepts exactly one
   message (EHLO/MAIL FROM/RCPT TO/DATA), then drops the connection instead
   of replying on QUIT - simulating a server that accepts delivery and then
   fails/disconnects during the close handshake.
   Returns [port received-promise stop-fn]."
  []
  (let [server-socket (ServerSocket. 0)
        port (.getLocalPort server-socket)
        received (promise)]
    (future
      (try
        (with-open [socket (.accept server-socket)
                    out (PrintWriter. (.getOutputStream socket))
                    in (BufferedReader. (InputStreamReader. (.getInputStream socket)))]
          (write-line out "220 fake-smtp ready")
          (loop [data-mode false
                 body (StringBuilder.)]
            (when-let [line (.readLine in)]
              (cond
                data-mode
                (if (= line ".")
                  (do
                    (deliver received (str body))
                    (write-line out "250 OK message accepted")
                    (recur false (StringBuilder.)))
                  (recur true (.append body (str line "\n"))))

                (re-find #"(?i)^EHLO" line)
                (do (write-line out "250-fake-smtp") (write-line out "250 OK") (recur false body))

                (re-find #"(?i)^MAIL FROM" line)
                (do (write-line out "250 OK") (recur false body))

                (re-find #"(?i)^RCPT TO" line)
                (do (write-line out "250 OK") (recur false body))

                (re-find #"(?i)^DATA" line)
                (do (write-line out "354 go ahead") (recur true body))

                (re-find #"(?i)^QUIT" line)
                ;; Force an abortive close (TCP RST) instead of replying
                ;; "221 bye" -- a graceful close/EOF here is tolerated by
                ;; JavaMail's SMTPTransport, but a reset while it's blocked
                ;; reading the QUIT reply raises a real IOException, exactly
                ;; like a server dropping the connection mid-handshake would.
                (.setSoLinger socket true 0)

                :else
                (recur false body)))))
        (catch Exception _ nil)))
    [port received (fn [] (.close server-socket))]))

(deftest send-message-succeeds-even-if-server-drops-connection-on-quit
  (testing "https://github.com/leihs/leihs/issues/2276: a message already accepted
            by the server must be reported as sent, even if the connection is then
            dropped during close/QUIT"
    (let [[port received stop] (fake-smtp-server)]
      (try
        (let [result (smtp/send-message
                      {:host "localhost" :port port}
                      {:from "sender@example.com"
                       :to "recipient@example.com"
                       :subject "test"
                       :body "hello"})]
          (is (some? (deref received 2000 nil))
              "the fake server must actually have received the message")
          (is (= 0 (:code result)))
          (is (= :SUCCESS (:error result))))
        (finally (stop))))))
