@echo off
REM Run Server Script for Windows
REM Starts the PaperMC server with optimized JVM settings

echo [INFO] Starting Colosseum Arena Server...

if not exist "server\eula.txt" (
    echo [ERROR] Server not set up. Run 'setup.bat [simple^|detailed]' first
    exit /b 1
)

call gradlew.bat runServer --no-daemon
