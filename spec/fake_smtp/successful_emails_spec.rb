require 'spec_helper'
require 'shared_spec'

describe 'Sending of emails succeeds (with domain)' do
  it '1st trial' do
    Setting.create(smtp_domain: SMTP_DOMAIN)

    email = FactoryBot.create(:email, :unsent)
    sleep(SEND_FREQUENCY_IN_SECONDS + 1)

    expect_until_timeout do
      email.reload
      expect(email.trials).to eq 1
      expect(email.code).to eq 0
      expect(email.error).to eq 'SUCCESS'
      expect(email.message).to eq 'messages sent'
      expect(Email.count).to eq 1
      assert_received_email(email.from_address, email.user.email)
      assert_domain(SMTP_DOMAIN)
    end
  end

  it '2nd trial' do
    email = FactoryBot.create(:email, :failed)
    sleep(SEND_FREQUENCY_IN_SECONDS + RETRY_FREQUENCY_IN_SECONDS + 1)

    expect_until_timeout do
      email.reload
      expect(email.trials).to eq 2
      expect(email.code).to eq 0
      expect(email.error).to eq 'SUCCESS'
      expect(email.message).to eq 'messages sent'
      expect(Email.count).to eq 1
      assert_received_email(email.from_address, email.user.email)
    end
  end

  it 'no trial if already succeeded' do
    email = FactoryBot.create(:email, :succeeded)
    sleep(SEND_FREQUENCY_IN_SECONDS + 10)

    email.reload
    expect(email.trials).to eq 1
    expect(email.code).to eq 0
    expect(email.error).to eq 'SUCCESS'
    expect(email.message).to eq 'message sent'

    expect(Email.count).to eq 1
    assert_not_received_email(email.from_address, email.user.email)
  end
end
