class SmtpSetting < Sequel::Model(:smtp_settings)
end

FactoryBot.define do
  factory :smtp_setting do
    default_from_address { "noreply@example.com" }

    trait :ms365_enabled do
      ms365_enabled { true }
      ms365_client_id { "test-client-id-#{SecureRandom.uuid}" }
      ms365_tenant_id { "test-tenant-id-#{SecureRandom.uuid}" }
      ms365_client_secret { "test-client-secret-#{SecureRandom.hex(32)}" }
      m365_token_url { "https://login.microsoftonline.com/{tenant_id}/oauth2/v2.0/token" }
      m365_graph_send_url { "https://graph.microsoft.com/v1.0/users/{user_id}/sendMail" }
    end
  end
end
