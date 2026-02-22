#!/bin/bash

# Fix server lock issues
# Usage: ./scripts/fix-locks.sh (or from project root)

# Change to project root if running from scripts directory
if [ -d "../templates" ]; then
    cd ..
fi

echo "[INFO] Fixing server locks..."

# Kill any running Java processes
pkill -9 -f "paper-1.21.4.jar" 2>/dev/null || true
pkill -9 java 2>/dev/null || true
sleep 2

# Remove session lock files
rm -f server/world/session.lock 2>/dev/null || true
rm -f server/world_nether/session.lock 2>/dev/null || true
rm -f server/world_the_end/session.lock 2>/dev/null || true

echo "[INFO] Locks cleared. You can now start the server."
echo "[INFO] Run: ./scripts/start-server.sh"
