class Setting < Sequel::Model(:settings)
end

FactoryBot.define do
  factory :setting do
    smtp_default_from_address { 'noreply@example.com' }
  end
end
