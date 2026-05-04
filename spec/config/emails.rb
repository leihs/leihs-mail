require "mail"

LEIHS_MAIL_SMTP_PORT = ENV.fetch("LEIHS_MAIL_SMTP_PORT", "32025")
SEND_FREQUENCY_IN_SECONDS = (ENV["LEIHS_MAIL_SEND_FREQUENCY_IN_SECONDS"] || 1).to_i
RETRIES_IN_SECONDS =
  (ENV["LEIHS_MAIL_RETRIES_IN_SECONDS"] && JSON.parse(ENV["LEIHS_MAIL_RETRIES_IN_SECONDS"])) \
  || [5, 10]

LOG_DIR = "#{Dir.pwd}/tmp/log"
LOG_FILE_PATH = "#{LOG_DIR}/fake_smtp.log"

Dir.mkdir(LOG_DIR) unless Dir.exist?(LOG_DIR)

RSpec.configure do |config|
  config.before :each do
    File.open(LOG_FILE_PATH, "w") { |file| file.truncate(0) }
  end
end

RSpec.configure do |config|
  config.before :suite do
    setup_email_client
  end
end

private

def setup_smtp_settings
  SmtpSetting.first.update(enabled: true, port: LEIHS_MAIL_SMTP_PORT)
  sleep 1.1 # due to memoized smtp settings in the mail app
end

def empty_mailbox
  Mail.delete_all
end

def setup_email_client
  $mail ||= Mail.defaults do # standard:disable Style/GlobalVars
    retriever_method(:pop3,
      address: ENV.fetch("LEIHS_MAIL_SMTP_ADDRESS", "localhost"),
      port: ENV.fetch("LEIHS_MAIL_POP3_PORT", "32110"),
      user_name: "any",
      password: "any",
      enable_ssl: false)
  end
end
