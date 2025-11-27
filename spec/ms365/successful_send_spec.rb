require "spec_helper"
require "shared_spec"

RSpec.describe "MS365 email sending", type: :request do
  before :each do
    # Enable MS365 in settings with URLs pointing to fake server
    fake_ms365_port = ENV.fetch("LEIHS_MAIL_FAKE_MS365_PORT", "9365")
    SmtpSetting.first.update(
      enabled: true,
      ms365_enabled: true,
      ms365_client_id: "test-client-id",
      ms365_tenant_id: "test-tenant-id",
      ms365_client_secret: "test-client-secret",
      m365_token_url: "http://localhost:#{fake_ms365_port}/{tenant_id}/oauth2/v2.0/token",
      m365_graph_send_url: "http://localhost:#{fake_ms365_port}/v1.0/users/{user_id}/sendMail"
    )
  end

  describe "successful email sending" do
    it "sends email via MS365 Graph API with valid token" do
      # Create mailbox with valid token
      mailbox = FactoryBot.create(:ms365_mailbox, id: "sender@example.com")

      # Create email to send
      email = FactoryBot.create(:email, :unsent, from_address: mailbox.id)

      # Wait for email to be processed
      sleep(SEND_FREQUENCY_IN_SECONDS + 1)

      expect_until_timeout do
        email.reload
        expect(email.trials).to eq 1
        expect(email.code).to eq 0
        expect(email.message).to eq "Email sent successfully via MS365 Graph API"
      end
    end

    it "refreshes expired token before sending" do
      # Create mailbox with expired token
      mailbox = FactoryBot.create(:ms365_mailbox, :expired_token, id: "sender@example.com")
      old_access_token = mailbox.access_token
      old_refresh_token = mailbox.refresh_token

      # Create email to send
      email = FactoryBot.create(:email, :unsent, from_address: mailbox.id)

      # Wait for email to be processed
      sleep(SEND_FREQUENCY_IN_SECONDS + 1)

      expect_until_timeout do
        email.reload
        mailbox.reload

        # Email should be sent successfully
        expect(email.trials).to eq 1
        expect(email.code).to eq 0
        expect(email.message).to eq "Email sent successfully via MS365 Graph API"

        # Mailbox should have new tokens (mock server generates random tokens)
        expect(mailbox.access_token).not_to eq old_access_token
        expect(mailbox.access_token).to start_with("fake_access_token_")
        expect(mailbox.token_expires_at).to be > Time.now
      end
    end
  end

  describe "failed email sending" do
    it "handles mailbox not found" do
      # Create email with sender that doesn't have MS365 mailbox
      email = FactoryBot.create(:email, :unsent, from_address: "nonexistent@example.com")

      # Wait for email to be processed
      sleep(SEND_FREQUENCY_IN_SECONDS + 1)

      expect_until_timeout do
        email.reload
        expect(email.trials).to eq 1
        expect(email.code).to eq 1
        expect(email.error).to eq "MS365_MAILBOX_NOT_FOUND"
      end
    end
  end
end
