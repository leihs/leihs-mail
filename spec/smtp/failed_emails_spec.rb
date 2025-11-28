require "spec_helper"
require "shared_spec"

describe "Sending of emails fails" do
  before :each do
    empty_mailbox
  end

  it "maximum trials reached" do
    email = FactoryBot.create(:email, :failed, trials: RETRIES_IN_SECONDS.length + 1)
    sleep(SEND_FREQUENCY_IN_SECONDS + RETRIES_IN_SECONDS.last + 1)

    expect_until_timeout do
      email_2 = email.dup.reload
      expect(email_2.trials).to eq email.trials
      expect(email_2.code).to eq email.code
      expect(email_2.error).to eq email.error
      expect(email_2.message).to eq email.message
      expect(Email.count).to eq 1
      assert_not_received_email(email.from_address, email.to_address)
    end
  end

  it "smtp server does not support TLS" do
    SmtpSetting.first.update(enable_starttls_auto: true)

    email = FactoryBot.create(:email, :unsent)

    sleep(SEND_FREQUENCY_IN_SECONDS + 1)

    expect_until_timeout do
      email.reload
      expect(email.trials).to be > 0
      expect(email.code).to eq 99
      expect(email.error).to eq "javax.mail.MessagingException"
      expect(email.message).to eq "STARTTLS is required but host does not support STARTTLS"
      expect(Email.count).to eq 1
      assert_not_received_email(email.from_address, email.to_address)
    end
  end

  it "fake sending if smtp disabled" do
    SmtpSetting.first.update(enabled: false)

    email = FactoryBot.create(:email, :unsent)
    sleep(SEND_FREQUENCY_IN_SECONDS + 1)

    expect_until_timeout do
      email.reload
      expect(email.trials).to eq 1
      expect(email.code).to eq 1
      expect(email.error).to eq "SMTP_DISABLED"
      expect(email.message).to eq "Message not sent because of disabled SMTP setting."
      expect(Email.count).to eq 1
      assert_not_received_email(email.from_address, email.to_address)
    end
  end
end
