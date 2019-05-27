SEND_FREQUENCY_IN_SECONDS = (ENV['LEIHS_MAIL_SEND_FREQUENCY_IN_SECONDS'] || 1).to_i
RETRIES_IN_SECONDS = (
  (ENV['LEIHS_MAIL_RETRIES_IN_SECONDS'] && JSON.parse(ENV['LEIHS_MAIL_RETRIES_IN_SECONDS'])) \
  || [5, 10]
)

LOG_FILE_PATH = "#{Dir.pwd}/log/fake_smtp.log"

RSpec.configure do |config|
  config.before :each  do
    File.open(LOG_FILE_PATH, 'w') {|file| file.truncate(0) }
  end
end

Dir.mkdir('fake-mailbox') unless Dir.exist?('fake-mailbox')
