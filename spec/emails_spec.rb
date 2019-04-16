require 'spec_helper'

describe 'Sending of emails' do
  context '1st trial' do
    it 'success' do
      FactoryBot.create(:email, :unsent)
    end
  end
end
