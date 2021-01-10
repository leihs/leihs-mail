class SmtpSetting < Sequel::Model(:smtp_settings)
end

FactoryBot.define do
  factory :smtp_setting do
    default_from_address { 'noreply@example.com' }
  end
end
