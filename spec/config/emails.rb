MAIL_SERVER_SMTP_PORT = ENV.fetch('LEIHS_MAIL_SMTP_PORT')

RSpec.configure do |config|
  config.before :each do
    update_smtp_settings
  end
end

private

def update_smtp_settings
  SmtpSetting.first.update(port: MAIL_SERVER_SMTP_PORT,
                           enabled: true)
end
