require 'spec_helper'
require 'shared_spec'

describe 'Sending of emails fails' do
  it 'User does not exist' do
    email = FactoryBot.create(:email, :unsent)
    email.user.destroy

    sleep(SEND_FREQUENCY_IN_SECONDS + 1)

    expect_until_timeout do
      email.reload
      expect(email.trials).to be > 0
      expect(email.code).to eq 99
      expect(email.error).to eq 'java.lang.Exception'
      expect(email.message).to eq 'message needs at least :from and :to or :from and :bcc'
      expect(Email.count).to eq 1
      assert_not_received_email(email.from_address, '')
    end
  end

  it 'maximum trials reached' do
    email = FactoryBot.create(:email, :failed, trials: RETRIES_IN_SECONDS.length + 1)
    sleep(SEND_FREQUENCY_IN_SECONDS + RETRIES_IN_SECONDS.last + 1)

    expect_until_timeout do
      email_2 = email.dup.reload
      expect(email_2.trials).to eq email.trials
      expect(email_2.code).to eq email.code
      expect(email_2.error).to eq email.error
      expect(email_2.message).to eq email.message
      expect(Email.count).to eq 1
      assert_not_received_email(email.from_address, email.user.email)
    end
  end

  it 'smtp server does not support TLS' do
    Setting.first.update(smtp_enable_starttls_auto: true)

    email = FactoryBot.create(:email, :unsent)

    sleep(SEND_FREQUENCY_IN_SECONDS + 1)

    expect_until_timeout do
      email.reload
      expect(email.trials).to be > 0
      expect(email.code).to eq 99
      expect(email.error).to eq 'javax.mail.MessagingException'
      expect(email.message).to eq 'STARTTLS is required but host does not support STARTTLS'
      expect(Email.count).to eq 1
      assert_not_received_email(email.from_address, email.user.email)
    end
  end
end
