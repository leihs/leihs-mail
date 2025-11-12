(ns dev.playground
  "Playground for testing MS365 SMTP/Graph API authentication"
  (:require
   [clojure.data.json :as json]
   [org.httpkit.client :as http]
   [taoensso.timbre :as log])
  (:import
   [java.util Properties]
   [javax.mail Message$RecipientType]
   [javax.mail Session]
   [javax.mail.internet InternetAddress MimeMessage]))

;; MS365 Credentials (from ./tmp/test-access-ms365.txt)

(def ms365-config
  {:client-id ":client-id"
   :tenant-id ":tenant-id"
   :client-secret ":client-secret"
   :scope "https://graph.microsoft.com/.default"})

;; ============================================================================
;; OAuth2 Token Acquisition
;; ============================================================================

(defn get-oauth2-token
  "Get OAuth2 access token using client credentials flow"
  [{:keys [client-id tenant-id client-secret scope]}]
  (let [token-url (str "https://login.microsoftonline.com/" tenant-id "/oauth2/v2.0/token")
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

    (log/debug "Token response status:" status)

    (if (= 200 status)
      (let [token-data (json/read-str body :key-fn keyword)]
        (log/debug "Successfully obtained access token")
        (log/debug "Token expires in:" (:expires_in token-data) "seconds")
        (:access_token token-data))
      (do
        (log/debug response)
        (log/error "Failed to get token. Status:" status)
        (log/error "Response:" body)
        nil))))

;; ============================================================================
;; Microsoft Graph API - Send Email
;; ============================================================================

(defn send-via-graph-api
  "Send email using Microsoft Graph API
   Requires Mail.Send permission on the app registration"
  [access-token from-address to-address subject body]
  (let [send-url (str "https://graph.microsoft.com/v1.0/users/" from-address "/sendMail")

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

    (log/debug "Graph API send response status:" status)

    (if (= 202 status)
      (do
        (log/debug "Email sent successfully via Graph API")
        {:success true :status status})
      (do
        (log/error "Failed to send email. Status:" status)
        (log/error "Response:" response-body)
        {:success false :status status :body response-body}))))

;; ============================================================================
;; SMTP with OAuth2 (XOAUTH2)
;; ============================================================================

(defn send-via-smtp-oauth2
  "Send email using SMTP with OAuth2 authentication (XOAUTH2)
   MS365 SMTP: smtp.office365.com:587"
  [access-token from-address to-address subject body]
  (let [props (doto (Properties.)
                (.put "mail.smtp.host" "smtp.office365.com")
                (.put "mail.smtp.port" "587")
                (.put "mail.smtp.starttls.enable" "true")
                (.put "mail.smtp.starttls.required" "true")
                (.put "mail.smtp.auth" "true")
                (.put "mail.smtp.auth.mechanisms" "XOAUTH2")
                (.put "mail.smtp.ssl.protocols" "TLSv1.2"))

        session (Session/getInstance props)
        message (doto (MimeMessage. session)
                  (.setFrom (InternetAddress. from-address))
                  (.setRecipients Message$RecipientType/TO
                                  (into-array InternetAddress [(InternetAddress. to-address)]))
                  (.setSubject subject)
                  (.setText body))

        transport (.getTransport session "smtp")]

    (try
      (.connect transport "smtp.office365.com" 587 from-address access-token)
      (log/debug "Connected to SMTP server with OAuth2")

      (.sendMessage transport message (.getAllRecipients message))
      (log/debug "Email sent successfully via SMTP OAuth2")

      (.close transport)
      {:success true}

      (catch Exception e
        (log/error e "Failed to send via SMTP OAuth2")
        (.close transport)
        {:success false :error (.getMessage e)}))))

;; ============================================================================
;; Test Functions
;; ============================================================================

(defn test-oauth2-token
  "Test getting an OAuth2 access token"
  []
  (log/debug "\n=== Testing OAuth2 Token Acquisition ===")
  (if-let [token (get-oauth2-token ms365-config)]
    (do
      (log/debug "✓ Successfully obtained access token")
      (log/debug "Token (first 50 chars):" (subs token 0 (min 50 (count token))) "...")
      token)
    (log/debug "✗ Failed to obtain access token")))

(defn test-graph-api-send
  "Test sending email via Microsoft Graph API
   IMPORTANT: Requires 'Mail.Send' permission configured in Azure AD app"
  [from-address to-address]
  (log/debug "\n=== Testing Email Send via Graph API ===")
  (if-let [token (get-oauth2-token ms365-config)]
    (do
      (log/debug "Sending test email...")
      (log/debug "From:" from-address)
      (log/debug "To:" to-address)
      (let [result (send-via-graph-api token
                                       from-address
                                       to-address
                                       "Test Email via MS365 Graph API"
                                       "This is a test email sent using Microsoft Graph API with OAuth2 authentication.")]
        (if (:success result)
          (log/debug "✓ Email sent successfully")
          (log/debug "✗ Failed to send email:" result))
        result))
    (log/debug "✗ Could not obtain access token")))

(defn test-smtp-oauth2-send
  "Test sending email via SMTP with OAuth2
   MS365 SMTP endpoint: smtp.office365.com:587"
  [from-address to-address]
  (log/debug "\n=== Testing Email Send via SMTP with OAuth2 ===")
  (if-let [token (get-oauth2-token ms365-config)]
    (do
      (log/debug "Sending test email...")
      (log/debug "From:" from-address)
      (log/debug "To:" to-address)
      (let [result (send-via-smtp-oauth2 token
                                         from-address
                                         to-address
                                         "Test Email via MS365 SMTP OAuth2"
                                         "This is a test email sent using MS365 SMTP with OAuth2 (XOAUTH2) authentication.")]
        (if (:success result)
          (log/debug "✓ Email sent successfully")
          (log/debug "✗ Failed to send email:" (:error result)))
        result))
    (log/debug "✗ Could not obtain access token")))

;; ============================================================================
;; Usage Examples
;; ============================================================================

(comment
  ;; 1. Test OAuth2 token acquisition
  (test-oauth2-token)

  ;; 2. Test sending via Graph API
  ;; Note: Requires Mail.Send permission in Azure AD app registration
  (test-graph-api-send "matus.kmit@functional.swiss" "matuskmit1@gmail.com")

  ;; 3. Test sending via SMTP with OAuth2
  ;; Note: Use the same email address that's configured in MS365
  (test-smtp-oauth2-send "matus.kmit@functional.swiss" "matuskmit1@gmail.com")

  ;; Get just the token for manual testing
  (def token (get-oauth2-token ms365-config))

  ;; Manual send via Graph API
  (send-via-graph-api token
                      "matus.kmit@functional.swiss"
                      "matuskmit1@gmail.com"
                      "Test Subject"
                      "Test Body")

  ;; Manual send via SMTP
  (send-via-smtp-oauth2 token
                        "matus.kmit@functional.swiss"
                        "matuskmit1@gmail.com"
                        "Test Subject"
                        "Test Body"))

;; ============================================================================
;; Notes and Setup Instructions
;; ============================================================================

(comment
  "SETUP CHECKLIST:

  1. Azure AD App Configuration:
     - App Registration already exists with Client ID and Tenant ID
     - Client Secret is configured

  2. Required API Permissions (in Azure Portal):

     For Graph API approach:
     - Microsoft Graph > Application permissions > Mail.Send
     - Grant admin consent for the tenant

     For SMTP OAuth2 approach:
     - Microsoft Graph > Application permissions > SMTP.Send
     - Grant admin consent for the tenant

  3. Mailbox Configuration:
     - The 'from-address' must be a valid mailbox in your MS365 tenant
     - For app-only auth (client credentials), you may need to configure
       ApplicationImpersonation role or use a service account

  4. Testing:
     - Start with test-oauth2-token to verify credentials
     - Then try test-graph-api-send or test-smtp-oauth2-send
     - Check Azure AD logs if authentication fails

  5. Common Issues:
     - 'Insufficient privileges': Check API permissions and admin consent
     - 'Mailbox not found': Verify the from-address exists in tenant
     - SMTP auth failures: Ensure SMTP.Send permission is granted

  RECOMMENDED APPROACH:
  - For application sending (no user context): Microsoft Graph API
  - For compatibility with existing SMTP code: SMTP with OAuth2
  ")
