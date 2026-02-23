@echo off
REM Run Server Script for Windows
REM Starts the PaperMC server with optimized JVM settings
REM Usage: scripts\run.bat (or from project root)
REM Smart Gradle Detection: Uses gradlew.bat if available, falls back to system gradle

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

echo [INFO] Starting Colosseum Arena Server...
echo [INFO] Using: %GRADLE_CMD%

if not exist "server\eula.txt" (
    echo [ERROR] Server not set up. Run 'scripts\setup.bat' first
    exit /b 1
)

call %GRADLE_CMD% runServer --no-daemon
