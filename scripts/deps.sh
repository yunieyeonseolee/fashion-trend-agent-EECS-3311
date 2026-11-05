#!/usr/bin/env bash
set -euo pipefail
# Sourced after launch scripts cd to the repository root.
mkdir -p build/deps
FASHION_GSON_JAR=build/deps/gson-2.13.2.jar
FASHION_GSON_SHA=dd0ce1b55a3ed2080cb70f9c655850cda86c206862310009dcb5e5c95265a5e0
if [ ! -f "$FASHION_GSON_JAR" ]; then
  curl -fsSL --connect-timeout 10 --max-time 60 \
    https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.13.2/gson-2.13.2.jar \
    -o "$FASHION_GSON_JAR.tmp"
  mv "$FASHION_GSON_JAR.tmp" "$FASHION_GSON_JAR"
fi
if command -v sha256sum >/dev/null 2>&1; then
  printf '%s  %s\n' "$FASHION_GSON_SHA" "$FASHION_GSON_JAR" | sha256sum -c - >/dev/null
else
  printf '%s  %s\n' "$FASHION_GSON_SHA" "$FASHION_GSON_JAR" | shasum -a 256 -c - >/dev/null
fi
FASHION_CP_SEPARATOR=:
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) FASHION_CP_SEPARATOR=';' ;; esac
