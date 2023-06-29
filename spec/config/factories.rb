require 'factory_bot'
require 'faker'
require 'pry'

Sequel::Model.db = database
Sequel::Model.send :alias_method, :save!, :save

RSpec.configure do |config|
  config.include FactoryBot::Syntax::Methods

  config.before(:suite) do
    FactoryBot.definition_file_paths = \
      %w{./database/spec/factories ./spec/factories}
    FactoryBot.find_definitions
  end
end
