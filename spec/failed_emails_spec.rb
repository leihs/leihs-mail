require 'spec_helper'

describe 'Sending of emails fails' do
  it 'SMTP server not reachable' do
  end

  it 'User does not exist' do
  end

  it 'maximum trials reached' do
    email = FactoryBot.create(:email, :failed, trials: 2)
    sleep(10)

    email_2 = email.reload
    expect(email_2.trials).to eq email.trials
    expect(email_2.code).to eq email.code
    expect(email_2.error).to eq email.error
    expect(email_2.message).to eq email.message

    expect(Email.count).to eq 1
  end
end
