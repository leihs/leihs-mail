class Email < Sequel::Model(:emails)
  many_to_one :user
end

FactoryBot.define do
  factory :email do
    user
    subject { Faker::Lorem.sentence }
    body { Faker::Lorem.paragraph }
    from_address { Faker::Internet.email }
  end

  trait :unsent do
    trials { 0 }
  end

  trait :succeeded do
    trials { 1 }
    code { 0 }
    error { 'SUCCESS' }
    message { 'message sent' }
  end

  trait :failed do
    trials { 1 }
    code { 69 }
    error { 'EX_UNAVAILABLE' }
    message { 'service unavailable' }
  end
end
