require 'spec_helper'
require 'shared_spec'

describe 'Sending of emails fails' do
  it 'SMTP server not reachable' do
    email = FactoryBot.create(:email, :unsent)

    sleep(SEND_FREQUENCY_IN_SECONDS + 1)
    email.reload
    expect(email.trials).to eq 1
    expect(email.code).to eq 99
    expect(email.error).to eq 'com.sun.mail.util.MailConnectException'
    expect(email.message).to eq "Couldn't connect to host, port: localhost, 25; timeout -1"
    expect(Email.count).to eq 1
    assert_not_received_email(email.sender, email.user.email)
  end
end