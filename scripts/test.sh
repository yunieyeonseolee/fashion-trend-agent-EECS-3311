#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/deps.sh
mkdir -p build/test-classes
find src/main/java src/test/java -name '*.java' -print > build/test-sources.txt
javac --release 17 -cp "$FASHION_GSON_JAR" -d build/test-classes @build/test-sources.txt
java -cp "build/test-classes${FASHION_CP_SEPARATOR}${FASHION_GSON_JAR}" fashion.DataLayerTest
java -cp "build/test-classes${FASHION_CP_SEPARATOR}${FASHION_GSON_JAR}" fashion.AnalysisTest
java -cp "build/test-classes${FASHION_CP_SEPARATOR}${FASHION_GSON_JAR}" fashion.ApplicationTest
java -Djava.awt.headless=true -cp "build/test-classes${FASHION_CP_SEPARATOR}${FASHION_GSON_JAR}" fashion.GuiTest
java -cp "build/test-classes${FASHION_CP_SEPARATOR}${FASHION_GSON_JAR}" fashion.LLMIntegrationTest
