#!/usr/bin/env bash

# Download tools that will be tested
wget -nc https://github.com/pinterest/ktlint/releases/download/0.39.0/ktlint
wget -nc https://github.com/cqfn/diKTat/releases/download/v0.6.2/diktat-0.6.2.jar

# Download latest release of SAVE CLI
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    wget -nc https://github.com/cqfn/save/releases/download/v0.1.0-alpha.2/save-0.1.0-alpha.2-linuxX64.kexe -O save
elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]]; then
    wget -nc https://github.com/cqfn/save/releases/download/v0.1.0-alpha.2/save-0.1.0-alpha.2-mingwX64.exe -O save
fi
# ... or copy locally built save-cli
# cp ../../save-cli/build/bin/linuxX64/releaseExecutable/save-0.1.0-alpha.2.66+20210609T095346Z-linuxX64.exe save

if [ ! -f save ]; then echo "ERROR: Please place save executable in $PWD before continuing" && exit 1; fi
chmod +x save

if [ "$1" == "--help" ]; then
    # todo: https://github.com/cqfn/save/issues/137
    echo 'Usage: ./run.sh [additional SAVE options].
For example, `./run.sh warn/chapter1/save.toml` to execute tests located in warn/chapter1
`./run.sh EnumValueSnakeCaseTest.kt to execute a single test`'
else
    ./save $@
fi
