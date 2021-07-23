#!/usr/bin/env bash

KTLINT_VERSION=0.39.0
DIKTAT_VERSION=1.0.0-rc.2
SAVE_VERSION=0.1.1

# Download tools that will be tested
if ! [ -f ktlint ]; then
  echo "Downloading ktlint-$KTLINT_VERSION"
  curl -sSLO -k https://github.com/pinterest/ktlint/releases/download/$KTLINT_VERSION/ktlint
fi

if ! [ -f diktat-$DIKTAT_VERSION.jar ]; then
  echo "Downloading diktat-$DIKTAT_VERSION"
  curl -sSLO -k https://github.com/cqfn/diKTat/releases/download/v$DIKTAT_VERSION/diktat-$DIKTAT_VERSION.jar
fi
