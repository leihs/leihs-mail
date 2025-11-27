class Ms365Mailbox < Sequel::Model(:ms365_mailboxes)
end

FactoryBot.define do
  factory :ms365_mailbox do
    id { Faker::Internet.email }
    access_token { "test_access_token_#{SecureRandom.hex(32)}" }
    refresh_token { "test_refresh_token_#{SecureRandom.hex(32)}" }
    token_expires_at { Time.now + 3600 } # 1 hour from now

    trait :expired_token do
      token_expires_at { Time.now - 3600 } # 1 hour ago
    end

    trait :expiring_soon do
      token_expires_at { Time.now + 240 } # 4 minutes from now (less than 5 min buffer)
    end
  end
end
