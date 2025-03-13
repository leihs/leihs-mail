require "spec_helper"
require "shared_spec"

describe "Sending of emails succeeds (with domain)" do
  before :each do
    empty_mailbox
  end

  it "1st trial" do
    domain = "example.com"
    SmtpSetting.first.update(domain: domain)

    email = FactoryBot.create(:email, :unsent)
    sleep(SEND_FREQUENCY_IN_SECONDS + 1)

    expect_until_timeout do
      email.reload
      expect(email.trials).to eq 1
      expect(email.code).to eq 0
      expect(email.error).to eq "SUCCESS"
      expect(email.message).to eq "messages sent"
      expect(Email.count).to eq 1
      assert_received_email(email.from_address, email.to_address)
      assert_domain(domain)
    end
  end

  it "2nd trial" do
    email = FactoryBot.create(:email, :failed)
    sleep(SEND_FREQUENCY_IN_SECONDS + RETRIES_IN_SECONDS[0] + 1)

    expect_until_timeout do
      email.reload
      expect(email.trials).to eq 2
      expect(email.code).to eq 0
      expect(email.error).to eq "SUCCESS"
      expect(email.message).to eq "messages sent"
      expect(Email.count).to eq 1
      assert_received_email(email.from_address, email.to_address)
    end
  end

  it "3nd trial" do
    email = FactoryBot.create(:email, :failed, trials: 2)
    sleep(SEND_FREQUENCY_IN_SECONDS + RETRIES_IN_SECONDS[1] + 1)

    expect_until_timeout do
      email.reload
      expect(email.trials).to eq 3
      expect(email.code).to eq 0
      expect(email.error).to eq "SUCCESS"
      expect(email.message).to eq "messages sent"
      expect(Email.count).to eq 1
      assert_received_email(email.from_address, email.to_address)
    end
  end

  it "no trial if already succeeded" do
    email = FactoryBot.create(:email, :succeeded)
    sleep(SEND_FREQUENCY_IN_SECONDS + RETRIES_IN_SECONDS[0])

    expect_until_timeout do
      email.reload
      expect(email.trials).to eq 1
      expect(email.code).to eq 0
      expect(email.error).to eq "SUCCESS"
      expect(email.message).to eq "message sent"

      expect(Email.count).to eq 1
      assert_not_received_email(email.from_address, email.to_address)
    end
  end
end
