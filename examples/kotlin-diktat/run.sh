#!/usr/bin/env bash
#wget -nc https://github.com/pinterest/ktlint/releases/download/0.39.0/ktlint
wget -nc https://repo1.maven.org/maven2/com/pinterest/ktlint/0.39.0/ktlint-0.39.0.jar
wget -nc https://github.com/cqfn/diKTat/releases/download/v0.6.1/diktat-0.6.1.jar
# Download latest release of SAVE CLI
#wget -nc https://github.com/cqfn/save/releases/download/v0.1.0-alpha.2/save-0.1.0-alpha.2-linuxX64.kexe -O save
# use this for mingw
#wget -nc https://github.com/cqfn/save/releases/download/v0.1.0-alpha.2/save-0.1.0-alpha.2-mingwX64.exe -O save
# ... or copy locally built save-cli
# cp ../../save-cli/build/bin/linuxX64/releaseExecutable/save-0.1.0-alpha.2.66+20210609T095346Z-linuxX64.exe save
if [ ! -f save ]; then echo "ERROR: Please place save executable in $PWD before continuing" && exit 1; fi
chmod +x save
./save --debug -prop save.properties
#./save --debug -prop save.properties "warn/MyTest.java" # todo: doesn't work? Uses the whole tree?