(ns leihs.mail.send.ms365
  (:require
   [clojure.data.json :as json]
   [clojure.string]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [java-time :as t]
   [leihs.mail.settings :as settings]
   [next.jdbc :as jdbc :refer [execute!] :rename {execute! jdbc-execute!}]
   [next.jdbc.sql :refer [query] :rename {query jdbc-query}]
   [org.httpkit.client :as http]
   [taoensso.timbre :as log]))

;;; Mailbox Management ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-mailbox
  "Get MS365 mailbox configuration for the given email address"
  [tx email-address]
  (-> (sql/select :*)
      (sql/from :ms365_mailboxes)
      (sql/where [:= :id email-address])
      sql-format
      (->> (jdbc-query tx) first)))

;;; OAuth2 Token Acquisition ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- replace-placeholder
  "Replace a placeholder in URL template with actual value"
  [url-template placeholder value]
  (clojure.string/replace url-template placeholder (str value)))

;;; Token Management ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn token-expired?
  "Check if access token is expired or will expire soon (within 5 minutes)"
  [token-expires-at]
  (if token-expires-at
    (let [now (t/instant)
          ;; Convert SQL Timestamp to Instant
          expires (.toInstant token-expires-at)
          buffer-minutes 5
          expires-with-buffer (t/minus expires (t/minutes buffer-minutes))]
      (t/after? now expires-with-buffer))
    true))  ; If no expiration time, consider it expired

(defn refresh-access-token
  "Refresh access token using refresh token"
  [refresh-token]
  (let [client-id (settings/ms365-client-id)
        tenant-id (settings/ms365-tenant-id)
        client-secret (settings/ms365-client-secret)
        token-url (replace-placeholder (settings/m365-token-url) "{tenant_id}" tenant-id)
        form-params {:client_id client-id
                     :refresh_token refresh-token
                     :client_secret client-secret
                     :grant_type "refresh_token"
                     :scope "https://graph.microsoft.com/Mail.Send"}

        response (deref (http/post token-url
                                   {:form-params form-params})
                        10000  ; timeout in ms (10 seconds)
                        {:error "timeout"})

        status (:status response)
        body (:body response)]

    (if (= 200 status)
      (let [token-data (json/read-str body :key-fn keyword)
            access-token (:access_token token-data)
            new-refresh-token (:refresh_token token-data)
            expires-in (:expires_in token-data)
            expires-at (t/plus (t/instant) (t/seconds expires-in))]
        {:access_token access-token
         :refresh_token (or new-refresh-token refresh-token)  ; Use new refresh token if provided
         :token_expires_at expires-at})
      (do
        (log/error "Failed to refresh MS365 token. Status:" status)
        (log/error "Response:" body)
        nil))))

(defn update-mailbox-token!
  "Update mailbox with new token information"
  [tx mailbox-id token-data]
  (-> (sql/update :ms365_mailboxes)
      (sql/set {:access_token (:access_token token-data)
                :refresh_token (:refresh_token token-data)
                :token_expires_at [:cast (:token_expires_at token-data) :timestamp]
                :updated_at [:now]})
      (sql/where [:= :id mailbox-id])
      sql-format
      (->> (jdbc-execute! tx))))

;;; Microsoft Graph API - Send Email ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn send-via-graph-api
  "Send email using Microsoft Graph API
   email-map should contain: :from :to :subject :body
   access-token: OAuth2 access token for the sender's mailbox"
  [email-map access-token]
  (let [from-address (:from email-map)
        to-address (:to email-map)
        subject (:subject email-map)
        body (:body email-map)
        send-url (replace-placeholder (settings/m365-graph-send-url) "{user_id}" from-address)

        email-payload {:message {:subject subject
                                 :body {:contentType "Text"
                                        :content body}
                                 :toRecipients [{:emailAddress {:address to-address}}]}
                       :saveToSentItems false}

        response (deref (http/post send-url
                                   {:headers {"Authorization" (str "Bearer " access-token)
                                              "Content-Type" "application/json"}
                                    :body (json/write-str email-payload)})
                        10000  ; timeout in ms (10 seconds)
                        {:error "timeout"})

        status (:status response)
        response-body (:body response)]

    (if (= 202 status)
      {:code 0
       :message "Email sent successfully via MS365 Graph API"}
      (do
        (log/error "Failed to send email via MS365. Status:" status)
        (log/error "Response:" response-body)
        (let [error-details (try
                              (let [parsed (json/read-str response-body :key-fn keyword)
                                    error-msg (-> parsed :error :message)
                                    error-code (-> parsed :error :code)]
                                (str error-code ": " error-msg))
                              (catch Exception _
                                response-body))]
          {:code 1
           :error :MS365_SEND_FAILED
           :message (str "MS365 API failed (Status " status "): " error-details)})))))
