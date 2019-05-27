def assert_received_email(from, to)
  s = "Received mail from <#{from}> with recipient <#{to}>"
  expect(system "grep '#{s}' #{LOG_FILE_PATH}").to be true
end

def assert_domain(domain)
  e = "EHLO #{domain}"
  expect(system "grep '#{e}' #{LOG_FILE_PATH}").to be true
end

def assert_not_received_email(from, to)
  s = "Received mail from <#{from}> with recipient <#{to}>"
  expect(system "grep '#{s}' #{LOG_FILE_PATH}").to be false
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
