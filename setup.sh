#!/bin/bash

# Setup Script
# Usage: ./setup.sh
# 
# Arena configuration is in phau.properties (created from phau.properties.defaults)
# Edit phau.properties after setup to customize:
#   - arena-base-y: Base Y level (default: 64)
#   - arena-type: simple or detailed (default: detailed)

echo "[INFO] Setting up Colosseum Arena..."
echo "[INFO] Template: phau.properties.defaults"
echo "[INFO] Edit server/phau.properties to change arena settings"

if ! ./gradlew setup --no-daemon; then
    echo "[ERROR] Setup failed"
    exit 1
fi

echo "[INFO] Setup complete!"
echo "[INFO] Edit server/phau.properties to change arena settings"
echo "[INFO] Run './start-server.sh' to start the server"
