@echo off
REM Arena Type Selection Script for Windows
REM Usage: setup.bat [simple|detailed]

set ARENA_TYPE=%1
if "%ARENA_TYPE%"=="" set ARENA_TYPE=detailed

if not "%ARENA_TYPE%"=="simple" if not "%ARENA_TYPE%"=="detailed" (
    echo [ERROR] Invalid arena type. Use 'simple' or 'detailed'
    echo Usage: setup.bat [simple^|detailed]
    exit /b 1
)

echo [INFO] Setting up Colosseum Arena with %ARENA_TYPE% arena...

REM Pass arena type as Gradle property
call gradlew.bat setup -ParenaType=%ARENA_TYPE% --no-daemon
if errorlevel 1 (
    echo [ERROR] Setup failed
    exit /b 1
)

echo [INFO] Setup complete!
echo [INFO] Arena type: %ARENA_TYPE%
echo [INFO] Run 'run.bat' or 'start-server.bat' to start the server
