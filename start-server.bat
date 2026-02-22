@echo off
REM Start Minecraft server with interactive console
REM Usage: start-server.bat

cd server || exit /b 1

if not exist "eula.txt" (
    echo [ERROR] Server not set up. Run 'setup.bat [simple^|detailed]' first
    exit /b 1
)

echo [INFO] Starting PaperMC server...
echo [INFO] Type 'help' for commands, 'stop' to shutdown
echo.

REM JVM Flags - see start-server.sh for documentation

java ^
    -Xms511M -Xmx511M ^
    -XX:+UseG1GC ^
    -XX:+ParallelRefProcEnabled ^
    -XX:MaxGCPauseMillis=200 ^
    -XX:+UnlockExperimentalVMOptions ^
    -XX:+DisableExplicitGC ^
    -XX:G1NewSizePercent=30 ^
    -XX:G1MaxNewSizePercent=40 ^
    -XX:G1HeapRegionSize=8M ^
    -XX:G1ReservePercent=20 ^
    -XX:G1HeapWastePercent=5 ^
    -Dpaper.disableWatchdog=true ^
    -Djava.awt.headless=true ^
    -jar paper-1.21.4.jar ^
    --nogui
