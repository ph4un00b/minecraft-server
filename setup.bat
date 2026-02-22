@echo off
REM Setup Script
REM Usage: setup.bat
REM 
REM Arena configuration is in phau.properties (created from phau.properties.defaults)
REM Edit phau.properties after setup to customize:
REM   - arena-base-y: Base Y level (default: 64)
REM   - arena-type: simple or detailed (default: detailed)

echo [INFO] Setting up Colosseum Arena...
echo [INFO] Template: phau.properties.defaults
echo [INFO] Edit server\phau.properties to change arena settings

call gradlew.bat setup --no-daemon
if errorlevel 1 (
    echo [ERROR] Setup failed
    exit /b 1
)

echo [INFO] Setup complete!
echo [INFO] Edit server\phau.properties to change arena settings
echo [INFO] Run 'start-server.bat' to start the server
