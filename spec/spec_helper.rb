require 'active_support/all'
require 'pathname'
MAIL_DIR = Pathname(__FILE__).join("../..")
require_relative MAIL_DIR.join('./database/spec/config/database')
require 'config/emails'
require 'config/factories'
require 'config/mail'
require 'pry'


RSpec.configure do |config|
  config.expect_with :rspec do |expectations|
    expectations.include_chain_clauses_in_custom_matcher_descriptions = true
  end

  config.mock_with :rspec do |mocks|
    mocks.verify_partial_doubles = true
  end

  config.shared_context_metadata_behavior = :apply_to_host_groups


  config.before(:example) do |example|
    srand 1
    db_clean
    db_restore_data seeds_sql
    update_smtp_settings
  end

end
