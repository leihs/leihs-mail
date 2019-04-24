#!/bin/sh -eux

files="$(git diff --relative --cached --name-only --diff-filter=ACM | grep -E "\.(clj|edn)$")"

boot_fmt_args=''
for F in $files; do
  boot_fmt_args="${boot_fmt_args} --files ${F}"
done

if test ! -z "$files"; then
  boot fmt $boot_fmt_args
  git add $files
fi
