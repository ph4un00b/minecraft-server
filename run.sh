#!/bin/bash

# Run Server Script
# Starts the PaperMC server with optimized JVM settings

echo "[INFO] Starting Colosseum Arena Server..."

if [ ! -f "server/eula.txt" ]; then
    echo "[ERROR] Server not set up. Run './setup.sh [simple|detailed]' first"
    exit 1
fi

./gradlew runServer --no-daemon
