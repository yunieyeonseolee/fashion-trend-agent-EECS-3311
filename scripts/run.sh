#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/deps.sh
mkdir -p build/classes
find src/main/java -name '*.java' -print > build/sources.txt
javac --release 17 -cp "$FASHION_GSON_JAR" -d build/classes @build/sources.txt
exec java -cp "build/classes${FASHION_CP_SEPARATOR}${FASHION_GSON_JAR}" fashion.cli.TrendCLI "$@"
