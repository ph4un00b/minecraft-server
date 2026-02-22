#!/bin/bash

# Setup Script
# Usage: ./setup.sh
# 
# Templates are in templates/ folder:
#   - templates/server.properties.defaults -> server/server.properties
#   - templates/phau.properties.defaults -> server/phau.properties
#
# After setup, edit files in server/ to customize:
#   - server/phau.properties: Arena settings (base Y, type)
#   - server/server.properties: Server settings (port, gamemode, etc)

echo "[INFO] Setting up Colosseum Arena..."
echo "[INFO] Templates: templates/ -> server/"
echo "[INFO] Edit server/phau.properties for arena settings"

if ! ./gradlew setup --no-daemon; then
    echo "[ERROR] Setup failed"
    exit 1
fi

echo "[INFO] Setup complete!"
echo "[INFO] Edit server/phau.properties to change arena settings"
echo "[INFO] Run './start-server.sh' to start the server"
