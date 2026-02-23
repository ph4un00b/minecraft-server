#!/bin/bash

# Run Server Script
# Starts the PaperMC server with optimized JVM settings
# Usage: ./scripts/run.sh (or from project root)
# Smart Gradle Detection: Uses ./gradlew if available, falls back to system gradle

# Change to project root if running from scripts directory
if [ -d "../templates" ]; then
    cd ..
fi

# Smart Gradle Detection Function
detect_gradle() {
    # Check if gradlew exists in current directory (project root)
    if [ -f "./gradlew" ]; then
        echo "./gradlew"
        return
    fi
    
    # Check if system gradle is available
    if command -v gradle &> /dev/null; then
        echo "gradle"
        return
    fi
    
    # Nothing found
    echo ""
}

# Detect which gradle to use
GRADLE_CMD=$(detect_gradle)

if [ -z "$GRADLE_CMD" ]; then
    echo "[ERROR] No Gradle found."
    echo "[ERROR] Please install Gradle:"
    echo "  Option 1: Download and extract gradlew to project root"
    echo "  Option 2: Install system gradle: sdk install gradle 8.11"
    exit 1
fi

echo "[INFO] Starting Colosseum Arena Server..."
echo "[INFO] Using: $GRADLE_CMD"

if [ ! -f "server/eula.txt" ]; then
    echo "[ERROR] Server not set up. Run './scripts/setup.sh' first"
    exit 1
fi

$GRADLE_CMD runServer --no-daemon
