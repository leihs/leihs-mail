# leihs-mail

## Development

### Start REPL

    ./bin/dev-run-backend

### Fake SMTP server

```
bundle install
./bin/run_fake_smtp
```

It respects the `LEIHS_MAIL_SMTP_PORT` (default 25) and `LEIHS_MAIL_POP3_PORT` (default 110) environmental variables.

## Uberjar

```
./bin/build
./bin/dev-run-uberjar

```

## Startup and configuration options

see

    ./bin/dev-run-backend --help


## Tests

### Tests with fake smtp server

```shell
# export run options in each terminal if necessary

# terminal 1
./bin/run_fake_smtp
# terminal 2
boot focus
# terminal 3
./bin/rspec spec/successful_emails_spec.rb spec/failed_emails_spec.rb
```

### Tests without fake smtp server

```shell
# export run stuff in each terminal if necessary

# terminal 1
boot focus
# terminal 2
./bin/rspec spec/smtp_server_unreachable_spec.rb
```
