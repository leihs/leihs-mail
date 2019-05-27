# leihs-mail

## Development

### Start REPL

`boot repl`

This throws you into `user` namespace (`dev/user.clj`). From here you can `(run)` the application (and pass run options if desired).

### Start watcher/reset task

`boot focus`

It watches for changed files, then reloads them using `clojure.tools.namespace` and resets the global application state as defined in `dev/reset.clj`. The only thing one has to do is to change a file and save it.

NOTE: Beware vim users! The reset blows away all your namespace definitions added interactively. You can disable this behaviour for a particular namespace by:

```clojure
(:require '[clojure.tools.namespace.repl :as ctnr]))
(ctnr/disable-reload!)
```

For more information see [clojure.tools.namespace](https://github.com/clojure/tools.namespace).

### Run -main

1. `boot run` given `export ...` (or accepting the defaults)
2. Or passing command line arguments `boot [ run -- -d ... ]` (square brackets due to [this](https://github.com/boot-clj/boot/wiki/Task-Options-DSL#positional-parameters))

### Fake SMTP server

```
bundle install
./scripts/run_fake_smtp
```

It respects the `LEIHS_MAIL_SMTP_PORT` (default 25) and `LEIHS_MAIL_POP3_PORT` (default 110) environmental variables.

## Uberjar

```
boot uberjar
java -jar target/leihs-mail.jar run
```

## Startup and configuration options

One can run the application with the following cli options or by setting the respective environmental variables:

```
-h --help
-d --database-url LEIHS_DATABASE_URL
   --send-frequency-in-seconds LEIHS_MAIL_SEND_FREQUENCY_IN_SECONDS
   --retries-in-seconds LEIHS_MAIL_RETRIES_IN_SECONDS
   --smtp-address LEIHS_MAIL_SMTP_ADDRESS
   --smtp-port LEIHS_MAIL_SMTP_PORT
```

The current defaults are to be found [here](https://github.com/leihs/leihs-mail/blob/master/src/all/leihs/mail/cli.clj).

The value of each option is basically determined in the following order:
1. cli option
2. environmental variable
3. default

`LEIHS_MAIL_SMTP_ADDRESS`, `LEIHS_MAIL_SMTP_PORT` and `LEIHS_MAIL_SMTP_DOMAIN` are handled specially. Their value is determined in the following order:
1. cli option
2. environmental variable
3. DB: `settings.smtp_*`
4. default

## Tests

### Tests with fake smtp server

```shell
# export run options in each terminal if necessary

# terminal 1
./scripts/run_fake_smtp
# terminal 2
boot focus
# terminal 3
bundle exec rspec spec/successful_emails_spec.rb spec/failed_emails_spec.rb
```

### Tests without fake smtp server

```shell
# export run stuff in each terminal if necessary

# terminal 1
boot focus
# terminal 2
bundle exec rspec spec/smtp_server_unreachable_spec.rb
```
