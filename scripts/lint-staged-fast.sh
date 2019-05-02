#!/bin/sh -ux

# https://github.com/kkinnear/zprint/blob/master/doc/graalvm.md

files="$(git diff --relative --cached --name-only --diff-filter=ACM | grep -E "\.(clj|edn)$")"

if test "$files" = ""; then
  exit 0
fi

for F in $files; do
  zprintm "$(cat .zprintrc)" < $F > "$F.new"
  mv "$F.new" $F
done

git add $files
