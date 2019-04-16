require 'spec_helper'

describe 'Sending of emails' do
  it '1st trial' do
    email = FactoryBot.create(:email, :unsent)
    sleep(10)

    email.reload
    expect(email.trials).to eq 1
    expect(email.code).to eq 0
    expect(email.error).to eq 'SUCCESS'
    expect(email.message).to eq 'message sent'
    
    expect(Email.count).to eq 1
  end

  it '2nd trial' do
    email = FactoryBot.create(:email, :failed)
    sleep(15)

    email.reload
    expect(email.trials).to eq 2
    expect(email.code).to eq 0
    expect(email.error).to eq 'SUCCESS'
    expect(email.message).to eq 'message sent'

    expect(Email.count).to eq 1
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
