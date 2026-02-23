#!/bin/bash

# Setup Script
# Usage: ./scripts/setup.sh (or from project root)
# Smart Gradle Detection: Uses ./gradlew if available, falls back to system gradle
#
# Templates are in templates/ folder:
#   - templates/server.properties.defaults -> server/server.properties
#   - templates/phau.properties.defaults -> server/phau.properties
#
# After setup, edit files in server/ to customize:
#   - server/phau.properties: Arena settings (base Y, type)
#   - server/server.properties: Server settings (port, gamemode, etc)

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

echo "[INFO] Setting up Colosseum Arena..."
echo "[INFO] Using: $GRADLE_CMD"
echo "[INFO] Templates: templates/ -> server/"
echo "[INFO] Edit server/phau.properties for arena settings"

if ! $GRADLE_CMD setup --no-daemon; then
    echo "[ERROR] Setup failed"
    exit 1
fi

echo "[INFO] Setup complete!"
echo "[INFO] Edit server/phau.properties to change arena settings"
echo "[INFO] In-game: /arena sety <level> to change Y level"
echo "[INFO] Run './scripts/start-server.sh' to start the server"
