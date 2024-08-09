SEND_FREQUENCY_IN_SECONDS = (ENV['LEIHS_MAIL_SEND_FREQUENCY_IN_SECONDS'] || 1).to_i
RETRIES_IN_SECONDS = (
  (ENV['LEIHS_MAIL_RETRIES_IN_SECONDS'] && JSON.parse(ENV['LEIHS_MAIL_RETRIES_IN_SECONDS'])) \
  || [5, 10]
)

LOG_DIR = "#{Dir.pwd}/tmp/log"
LOG_FILE_PATH = "#{LOG_DIR}/fake_smtp.log"
FAKE_MAILBOX_DIR = "#{Dir.pwd}/tmp/fake-mailbox"

Dir.mkdir(LOG_DIR) unless Dir.exist?(LOG_DIR) 

RSpec.configure do |config|
  config.before :each  do
    File.open(LOG_FILE_PATH, 'w') {|file| file.truncate(0) }
  end
end

Dir.mkdir(FAKE_MAILBOX_DIR) unless Dir.exist?(FAKE_MAILBOX_DIR)
