MAIL_SERVER_SMTP_PORT = ENV.fetch('LEIHS_MAIL_SMTP_PORT', 25)

def update_smtp_settings
  SmtpSetting.first.update(port: MAIL_SERVER_SMTP_PORT,
                           enabled: true)
end
