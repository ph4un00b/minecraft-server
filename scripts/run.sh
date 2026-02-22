#!/bin/bash

# Run Server Script
# Starts the PaperMC server with optimized JVM settings
# Usage: ./scripts/run.sh (or from project root)

# Change to project root if running from scripts directory
if [ -d "../templates" ]; then
    cd ..
fi

echo "[INFO] Starting Colosseum Arena Server..."

if [ ! -f "server/eula.txt" ]; then
    echo "[ERROR] Server not set up. Run './scripts/setup.sh' first"
    exit 1
fi

./gradlew runServer --no-daemon
