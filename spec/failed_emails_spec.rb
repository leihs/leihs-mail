require 'spec_helper'
require 'shared_spec'

describe 'Sending of emails fails' do
  it 'User does not exist' do
    email = FactoryBot.create(:email, :unsent)
    email.user.destroy

    sleep(SEND_FREQUENCY_IN_SECONDS + 1)
    email.reload
    expect(email.trials).to be > 0
    expect(email.code).to eq 99
    expect(email.error).to eq 'java.lang.Exception'
    expect(email.message).to eq 'message needs at least :from and :to or :from and :bcc'
    expect(Email.count).to eq 1
    assert_not_received_email(email.sender, '')
  end

  it 'maximum trials reached' do
    email = FactoryBot.create(:email, :failed, trials: 2)
    sleep(SEND_FREQUENCY_IN_SECONDS + 1)

    email_2 = email.dup.reload
    expect(email_2.trials).to eq email.trials
    expect(email_2.code).to eq email.code
    expect(email_2.error).to eq email.error
    expect(email_2.message).to eq email.message
    expect(Email.count).to eq 1
    assert_not_received_email(email.sender, email.user.email)
  end
end
