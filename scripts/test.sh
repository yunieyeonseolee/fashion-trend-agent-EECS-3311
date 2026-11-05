#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/test-classes
find src/main/java src/test/java -name '*.java' -print > build/test-sources.txt
javac --release 17 -d build/test-classes @build/test-sources.txt
java -cp build/test-classes fashion.DataLayerTest
java -cp build/test-classes fashion.AnalysisTest
