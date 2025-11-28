require "sinatra/base"
require "json"

class FakeMs365Server < Sinatra::Base
  set :port, ENV.fetch("LEIHS_MAIL_FAKE_MS365_PORT", "9365")
  set :bind, "127.0.0.1"

  # Token refresh endpoint
  # Matches: /{tenant_id}/oauth2/v2.0/token
  post "/:tenant_id/oauth2/v2.0/token" do
    content_type :json

    # Return a fake token response
    {
      access_token: "fake_access_token_#{SecureRandom.hex(32)}",
      refresh_token: "fake_refresh_token_#{SecureRandom.hex(32)}",
      expires_in: 3600,
      token_type: "Bearer"
    }.to_json
  end

  # Send email endpoint
  # Matches: /v1.0/users/{user_id}/sendMail
  post "/v1.0/users/:user_id/sendMail" do
    content_type :json

    # Validate Authorization header
    auth_header = request.env["HTTP_AUTHORIZATION"]
    if auth_header.nil? || !auth_header.start_with?("Bearer ")
      status 401
      return {
        error: {
          code: "InvalidAuthenticationToken",
          message: "Access token is missing or invalid"
        }
      }.to_json
    end

    # Success - return 202 Accepted
    status 202
    ""
  end

  # Health check
  get "/health" do
    "OK"
  end
end

if __FILE__ == $0
  FakeMs365Server.run!
end
