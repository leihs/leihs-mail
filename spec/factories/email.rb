class Email < Sequel::Model(:emails)
  many_to_one :user
  many_to_one :inventory_pool
end

FactoryBot.define do
  factory :email do
    subject { Faker::Lorem.sentence }
    body { Faker::Lorem.paragraph }
    from_address { Faker::Internet.email }

    after(:build) do |email|
      if [true, false].sample
        user = create(:user)
        email.user_id = user.id
        email.to_address = user.email
      else
        pool = create(:inventory_pool)
        email.inventory_pool_id = pool.id
        email.to_address = pool.email
      end
    end
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
