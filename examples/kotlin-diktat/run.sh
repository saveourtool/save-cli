#!/usr/bin/env bash
#wget -nc https://github.com/pinterest/ktlint/releases/download/0.39.0/ktlint
wget -nc https://repo1.maven.org/maven2/com/pinterest/ktlint/0.39.0/ktlint-0.39.0.jar
wget -nc https://github.com/cqfn/diKTat/releases/download/v0.6.1/diktat-0.6.1.jar
wget -nc https://github.com/cqfn/save/releases/download/v0.1.0-alpha.2/save-0.1.0-alpha.2-linuxX64.kexe -O save
# use this for mingw
#wget -nc https://github.com/cqfn/save/releases/download/v0.1.0-alpha.2/save-0.1.0-alpha.2-mingwX64.exe -O save
# cp save-0.1.0-alpha.2.66+20210609T095346Z-mingwX64.exe save
chmod +x save
./save --debug -prop save.properties
#./save --debug -prop save.properties "warn/MyTest.java" # todo: doesn't work? Uses the whole tree?