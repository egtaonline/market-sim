#!/bin/bash

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    echo "usage: $0 [-h] simulator-name"
    echo
    echo "Creates an EGTA simulator zip named simulator-name using environment configuration defaults specified in <simulator-name>.json. If no <simulator-name>.json is specified, simulator creation will fail. The configuration file must be in the /config directory and must have the same name as your simulator. Note that it will be renamed to defaults.json in the simulator zip." | fold -s
    exit 1
fi

SKELETON="egta"
LIBS="lib"
JAR="dist/hft.jar"
NAME="${1%.zip}"
DEFAULTS="config/$NAME.json"

if [[ -e "$NAME" ]]; then
    echo "Error: Can't create a simulator with the same name as an existing directory"
    exit 1
fi
if [[ ! -e "$JAR" ]]; then
    echo "Error: Can't find hft.jar in /dist directory.  Run build-jar from /build.xml to generate it."
    exit 1
fi
if [[ ! -e "$DEFAULTS" ]]; then
    echo "Error: Can't find $NAME.json in /config directory."
    exit 1
fi

ant
cp -rv "$SKELETON" "$NAME"
cp -v "$DEFAULTS" "$NAME/defaults.json"
mkdir "$NAME/$LIBS"
cp -v "$LIBS/"* "$NAME/$LIBS"
cp -v "$JAR" "$NAME"
zip -v -r "$NAME" "$NAME"
rm -rf "$NAME"
