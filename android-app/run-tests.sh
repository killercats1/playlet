#!/usr/bin/env bash
# Runs test/ParserTest.java on the JVM against the app's pure classes (Parser, Format, Video, VideoDetails).
set -euo pipefail
cd "$(dirname "$0")"
OUT=build/test
mkdir -p "$OUT"
JSON_JAR=build/json-20240303.jar
[ -f "$JSON_JAR" ] || curl -fsSL -o "$JSON_JAR" https://repo1.maven.org/maven2/org/json/json/20240303/json-20240303.jar
P=src/com/killercats/playletmobile
javac -nowarn -encoding UTF-8 -cp "$JSON_JAR" -d "$OUT" $P/Parser.java $P/Format.java $P/Video.java $P/VideoDetails.java test/ParserTest.java
java -cp "$OUT:$JSON_JAR" ParserTest
