#!/bin/sh -ux

files="$(git diff --relative --cached --name-only --diff-filter=ACM | grep -E "\.(clj|edn)$")"

if test "$files" = ""; then
  exit 0
fi

boot_fmt_args=''
for F in $files; do
  boot_fmt_args="${boot_fmt_args} --files ${F}"
done

boot fmt $boot_fmt_args || exit 1
git add $files
