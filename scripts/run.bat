@echo off
REM Run Server Script for Windows
REM Starts the PaperMC server with optimized JVM settings
REM Usage: scripts\run.bat (or from project root)

REM Change to project root if running from scripts directory
if exist "..\templates" (
    cd ..
)

echo [INFO] Starting Colosseum Arena Server...

if not exist "server\eula.txt" (
    echo [ERROR] Server not set up. Run 'scripts\setup.bat' first
    exit /b 1
)

call gradlew.bat runServer --no-daemon
