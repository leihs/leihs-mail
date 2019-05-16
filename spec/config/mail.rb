SEND_FREQUENCY_IN_SECONDS = (ENV['LEIHS_MAIL_SEND_FREQUENCY_IN_SECONDS'] || 5).to_i
RETRY_FREQUENCY_IN_SECONDS = (ENV['LEIHS_MAIL_RETRY_FREQUENCY_IN_SECONDS'] || 10).to_i
MAXIMUM_TRIALS = (ENV['LEIHS_MAIL_MAXIMUM_TRIALS'] || 2).to_i

LOG_FILE_PATH = "#{Dir.pwd}/log/fake_smtp.log"

RSpec.configure do |config|
  config.before :each  do
    File.open(LOG_FILE_PATH, 'w') {|file| file.truncate(0) }
  end
end

Dir.mkdir('fake-mailbox') unless Dir.exist?('fake-mailbox')
