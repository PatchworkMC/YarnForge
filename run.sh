#!/bin/sh
./gradlew setup --no-daemon
./gradlew forgeRemapYarn --mc-version 1.16.4 --mappings "net.fabricmc:yarn:1.16.4+build.7" --no-daemon
rm -rf ./src/main/java
mv ./remapped/main ./src/main/java/
rm -rf ./src/test/java
mv ./remapped/test ./src/test/java
rm -rf ./projects/clean/src/main/java
mv ./remapped/clean ./projects/clean/src/main/java
rm -rf ./projects/forge/src/main/java
mv ./remapped/patched ./projects/forge/src/main/java