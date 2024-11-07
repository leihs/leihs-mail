def assert_received_email(from, to)
  expect(
    Mail.all.filter do |m|
      m.from == [from] && m.to == [to]
    end.count
  ).to eq 1
end

def assert_domain(domain)
  e = "EHLO #{domain}"
  expect(system "grep '#{e}' #{LOG_FILE_PATH}").to be true
end

def assert_not_received_email(from, to)
  expect(
    Mail.all.any? do |m|
      m.from_address == [from] && m.to_address == [to]
    end
  ).to be false
end

def expect_until_timeout(timeout = 15)
  Timeout.timeout(timeout) do
    p = proc do
      begin 
        aggregate_failures do
          yield
        end
      rescue RSpec::Expectations::MultipleExpectationsNotMetError => e
        sleep 1
        p.call
      end
    end

    p.call
  end
end
