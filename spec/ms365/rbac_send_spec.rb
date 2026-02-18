require "spec_helper"
require "shared_spec"

RSpec.describe "MS365 RBAC email sending", type: :request do
  before :each do
    fake_ms365_port = ENV.fetch("LEIHS_MAIL_FAKE_MS365_PORT", "9365")
    SmtpSetting.first.update(
      enabled: true,
      ms365_enabled: true,
      ms365_auth_mode: "rbac",
      ms365_client_id: "test-client-id",
      ms365_tenant_id: "test-tenant-id",
      ms365_client_secret: "test-client-secret",
      ms365_token_url: "http://localhost:#{fake_ms365_port}/{tenant_id}/oauth2/v2.0/token",
      ms365_graph_send_url: "http://localhost:#{fake_ms365_port}/v1.0/users/{user_id}/sendMail"
    )
  end

  # NOTE: this test must run first — it relies on the RBAC token cache being empty
  # (no token acquired yet in this service process). The success test below fills the cache.
  it "handles RBAC token acquisition failure" do
    SmtpSetting.first.update(
      ms365_token_url: "http://localhost:1/{tenant_id}/oauth2/v2.0/token"
    )

    email = FactoryBot.create(:email, :unsent, from_address: "sender@example.com")

    sleep(SEND_FREQUENCY_IN_SECONDS + 1)

    expect_until_timeout do
      email.reload
      expect(email.trials).to eq 1
      expect(email.code).to eq 1
      expect(email.error).to eq "MS365_RBAC_TOKEN_FAILED"
    end
  end

  it "sends email via RBAC without mailbox lookup" do
    email = FactoryBot.create(:email, :unsent, from_address: "sender@example.com")

    sleep(SEND_FREQUENCY_IN_SECONDS + 1)

    expect_until_timeout do
      email.reload
      expect(email.trials).to eq 1
      expect(email.code).to eq 0
      expect(email.message).to eq "Email sent successfully via MS365 Graph API"
    end
  end
end
