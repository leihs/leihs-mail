(ns leihs.mail.send.ms365
  (:require
   [clojure.data.json :as json]
   [leihs.mail.settings :as settings]
   [org.httpkit.client :as http]
   [taoensso.timbre :as log]))

;;; OAuth2 Token Acquisition ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-oauth2-token
  "Get OAuth2 access token using client credentials flow"
  []
  (let [client-id (settings/ms365-client-id)
        tenant-id (settings/ms365-tenant-id)
        client-secret (settings/ms365-client-secret)
        scope "https://graph.microsoft.com/.default"
        token-url (str "https://login.microsoftonline.com/" tenant-id "/oauth2/v2.0/token")
        form-params {:client_id client-id
                     :scope scope
                     :client_secret client-secret
                     :grant_type "client_credentials"}

        response (deref (http/post token-url
                                   {:form-params form-params})
                        10000  ; timeout in ms (10 seconds)
                        {:error "timeout"})

        status (:status response)
        body (:body response)]

    (log/debug "MS365 token response status:" status)

    (if (= 200 status)
      (let [token-data (json/read-str body :key-fn keyword)]
        (log/debug "Successfully obtained MS365 access token")
        (log/debug "Token expires in:" (:expires_in token-data) "seconds")
        (:access_token token-data))
      (do
        (log/error "Failed to get MS365 token. Status:" status)
        (log/error "Response:" body)
        nil))))

;;; Microsoft Graph API - Send Email ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn send-via-graph-api
  "Send email using Microsoft Graph API
   email-map should contain: :from :to :subject :body"
  [email-map]
  (if-let [access-token (get-oauth2-token)]
    (let [from-address (:from email-map)
          to-address (:to email-map)
          subject (:subject email-map)
          body (:body email-map)
          send-url (str "https://graph.microsoft.com/v1.0/users/" from-address "/sendMail")

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

      (log/debug "MS365 Graph API send response status:" status)

      (if (= 202 status)
        (do
          (log/info "Email sent successfully via MS365 Graph API to:" to-address)
          {:code 0
           :message "Email sent successfully via MS365 Graph API"})
        (do
          (log/error "Failed to send email via MS365. Status:" status)
          (log/error "Response:" response-body)
          {:code 1
           :error :MS365_SEND_FAILED
           :message (str "Failed to send via MS365 Graph API. Status: " status)
           :response response-body})))
    (do
      (log/error "Failed to obtain MS365 access token")
      {:code 1
       :error :MS365_TOKEN_FAILED
       :message "Failed to obtain MS365 access token"})))
