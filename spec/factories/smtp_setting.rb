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
    end

    trait :ms365_rbac do
      ms365_enabled { true }
      ms365_auth_mode { "rbac" }
      ms365_client_id { "test-client-id-#{SecureRandom.uuid}" }
      ms365_tenant_id { "test-tenant-id-#{SecureRandom.uuid}" }
      ms365_client_secret { "test-client-secret-#{SecureRandom.hex(32)}" }
    end
  end
end
