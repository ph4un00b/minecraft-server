@echo off
REM Setup Script
REM Usage: scripts\setup.bat (or from project root)
REM
REM Templates are in templates\ folder:
REM   - templates\server.properties.defaults -> server\server.properties
REM   - templates\phau.properties.defaults -> server\phau.properties
REM

REM Change to project root if running from scripts directory
if exist "..\templates" (
    cd ..
)

echo [INFO] Setting up Colosseum Arena...
echo [INFO] Templates: templates\ -> server\
echo [INFO] Edit server\phau.properties for arena settings

call gradlew.bat setup --no-daemon
if errorlevel 1 (
    echo [ERROR] Setup failed
    exit /b 1
)

echo [INFO] Setup complete!
echo [INFO] Edit server\phau.properties to change arena settings
echo [INFO] In-game: /arena sety ^<level^> to change Y level
echo [INFO] Run 'scripts\start-server.bat' to start the server
