require 'spec_helper'

def assert_received_email(from, to)
  s = "Received mail from <#{from}> with recipient <#{to}>"
  expect(system "grep '#{s}' #{LOG_FILE_PATH}").to be true
end

describe 'Sending of emails' do
  it '1st trial' do
    email = FactoryBot.create(:email, :unsent)
    sleep(SEND_FREQUENCY_IN_SECONDS + DELAY_LATENCY_IN_SECONDS)

    email.reload
    expect(email.trials).to eq 1
    expect(email.code).to eq 0
    expect(email.error).to eq 'SUCCESS'
    expect(email.message).to eq 'messages sent'
    
    expect(Email.count).to eq 1
    assert_received_email(email.sender, email.user.email)
  end

  it '2nd trial' do
    email = FactoryBot.create(:email, :failed)
    sleep(RETRY_FREQUENCY_IN_SECONDS + DELAY_LATENCY_IN_SECONDS)

    email.reload
    expect(email.trials).to eq 2
    expect(email.code).to eq 0
    expect(email.error).to eq 'SUCCESS'
    expect(email.message).to eq 'messages sent'

    expect(Email.count).to eq 1
    assert_received_email(email.sender, email.user.email)
  end

  it 'no trial if already succeeded' do
    email = FactoryBot.create(:email, :succeeded)
    sleep(10)

    email.reload
    expect(email.trials).to eq 1
    expect(email.code).to eq 0
    expect(email.error).to eq 'SUCCESS'
    expect(email.message).to eq 'message sent'

    expect(Email.count).to eq 1
  end
end
