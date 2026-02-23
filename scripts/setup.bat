@echo off
REM Setup Script
REM Usage: scripts\setup.bat (or from project root)
REM Smart Gradle Detection: Uses gradlew.bat if available, falls back to system gradle
REM
REM Templates are in templates\ folder:
REM   - templates\server.properties.defaults -> server\server.properties
REM   - templates\phau.properties.defaults -> server\phau.properties

REM Change to project root if running from scripts directory
if exist "..\templates" (
    cd ..
)

REM Smart Gradle Detection
if exist "gradlew.bat" (
    set GRADLE_CMD=gradlew.bat
) else (
    where gradle >nul 2>&1
    if %errorlevel% == 0 (
        set GRADLE_CMD=gradle
    ) else (
        echo [ERROR] No Gradle found.
        echo [ERROR] Please install Gradle:
        echo   Option 1: Download gradlew.bat to project root
        echo   Option 2: Install system gradle from https://gradle.org/install/
        exit /b 1
    )
)

echo [INFO] Setting up Colosseum Arena...
echo [INFO] Using: %GRADLE_CMD%
echo [INFO] Templates: templates\ -> server\
echo [INFO] Edit server\phau.properties for arena settings

call %GRADLE_CMD% setup --no-daemon
if errorlevel 1 (
    echo [ERROR] Setup failed
    exit /b 1
)

echo [INFO] Setup complete!
echo [INFO] Edit server\phau.properties to change arena settings
echo [INFO] In-game: /arena sety ^<level^> to change Y level
echo [INFO] Run 'scripts\start-server.bat' to start the server
