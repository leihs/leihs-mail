class User < Sequel::Model
end

FactoryBot.modify do
  factory :user do
    organization { Faker::Lorem.characters(8) }
  end
end
