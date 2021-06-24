#!/usr/bin/env bash

KTLINT_VERSION=0.39.0
DIKTAT_VERSION=0.6.3
SAVE_VERSION=0.1.0-alpha.2

# Download tools that will be tested
if ! [ -f ktlint ]; then
  echo "Downloading ktlint-$KTLINT_VERSION"
  curl -sSLO https://github.com/pinterest/ktlint/releases/download/$KTLINT_VERSION/ktlint
fi

if ! [ -f diktat-$DIKTAT_VERSION.jar ]; then
  echo "Downloading diktat-$DIKTAT_VERSION"
  curl -sSLO https://github.com/cqfn/diKTat/releases/download/v$DIKTAT_VERSION/diktat-$DIKTAT_VERSION.jar
fi

if ! [ -f save ]; then
  echo "Downloading SAVE"
  # Download latest release of SAVE CLI
  if [[ "$OSTYPE" == "linux-gnu"* ]]; then
      curl -sSL https://github.com/cqfn/save/releases/download/v$SAVE_VERSION/save-$SAVE_VERSION-linuxX64.kexe -o save
  elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]]; then
      curl -sSL https://github.com/cqfn/save/releases/download/v$SAVE_VERSION/save-$SAVE_VERSION-mingwX64.exe -o save
  fi
fi
# ... or copy locally built save-cli
# cp ../../save-cli/build/bin/linuxX64/releaseExecutable/save-$SAVE_VERSION.66+20210609T095346Z-linuxX64.exe save

if [ ! -f save ]; then echo "ERROR: Please place save executable in $PWD before continuing" && exit 1; fi
chmod +x save

if [ "$1" == "--help" ]; then
    # todo: https://github.com/cqfn/save/issues/137
    echo 'Usage: ./run.sh [additional SAVE options].
For example, `./run.sh warn/chapter1/save.toml` to execute tests located in warn/chapter1
`./run.sh EnumValueSnakeCaseTest.kt to execute a single test`'
else
    ./save --debug -prop save.properties $1
fi
