#!/bin/bash

# Arena Type Selection Script
# Usage: ./setup.sh [simple|detailed]

ARENA_TYPE="${1:-detailed}"

if [ "$ARENA_TYPE" != "simple" ] && [ "$ARENA_TYPE" != "detailed" ]; then
    echo "[ERROR] Invalid arena type. Use 'simple' or 'detailed'"
    echo "Usage: ./setup.sh [simple|detailed]"
    exit 1
fi

echo "[INFO] Setting up Colosseum Arena with $ARENA_TYPE arena..."
export ARENA_TYPE="$ARENA_TYPE"

if ! ./gradlew setup --no-daemon; then
    echo "[ERROR] Setup failed"
    exit 1
fi

echo "[INFO] Setup complete!"
echo "[INFO] Arena type: $ARENA_TYPE"
echo "[INFO] Run './run.sh' to start the server"
