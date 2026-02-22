# Colosseum Arena

A self-contained, offline-mode PaperMC 1.21.4 Minecraft server with a Kotlin-based plugin that auto-generates a gothic colosseum arena at world spawn.

## Features

- **Zero Configuration**: Runs out of the box
- **Two Arena Types**: Simple or detailed gothic architecture
- **Deterministic Build**: Same arena every time, builds once only
- **Cross-Platform**: Linux, macOS, and Windows support
- **Clean Architecture**: Server files isolated in `server/` folder

## Prerequisites

- **Java 21** (JDK required)
- **Gradle** (if wrapper files not committed)
- **Network** (only for initial PaperMC download)

### Java Version Check

```bash
java -version
# Should output: openjdk version "21"...
```

If Java 21 is not installed, the setup will fail with:
```
[ERROR] Java 21 is required. Detected: <version>. Please install JDK 21.
```

## Quick Start

### Linux / macOS

```bash
# Setup with simple arena
./setup.sh simple

# Setup with detailed gothic arena (default)
./setup.sh detailed

# Start the server
./run.sh
```

### Windows

```cmd
REM Setup with simple arena
setup.bat simple

REM Setup with detailed gothic arena (default)
setup.bat detailed

REM Start the server
run.bat
```

## Arena Types

### Simple Arena
- Perfect for testing or low-resource environments
- Basic circular wall with single gate
- ~5,000 blocks
- Build time: ~2 seconds

### Detailed Arena (Default)
- Full gothic architecture
- Thick walls (7.5 blocks thick)
- 7 buttresses (E, SE, S, SW, W, NW, NE)
- 8 windows at 22.5° offsets
- North gate with towers
- Crenellations and floor pattern
- ~45,000 blocks
- Build time: ~10 seconds

## Project Structure

```
root/
├── .gitignore
├── README.md
├── setup.sh / setup.bat        # Setup scripts
├── run.sh / run.bat            # Server run scripts
├── build.gradle.kts            # Main build configuration
├── settings.gradle.kts         # Project settings
├── server/                     # Server files (generated)
│   ├── world/                  # World data
│   ├── plugins/                # Plugin JAR
│   ├── paper-1.21.4.jar        # Paper server
│   ├── server.properties       # Server config
│   └── eula.txt                # EULA acceptance
├── external/                   # Downloaded Paper JAR
├── plugins/                    # Compiled plugin
└── src/main/kotlin/com/colosseum/arena/
    └── ArenaPlugin.kt          # Main plugin
```

## Gradle Tasks

```bash
# Complete setup
./gradlew setup

# Download PaperMC server
./gradlew downloadPaper

# Build plugin only
./gradlew jar

# Run server
./gradlew runServer

# Delete world (regenerate arena)
./gradlew cleanWorld

# Validate Java version
./gradlew checkJava
```

## First Run Expectations

1. **Setup Phase** (~1-2 minutes):
   - Java validation
   - PaperMC download (requires network)
   - Plugin compilation
   - Server initialization
   - EULA auto-acceptance

2. **First Server Start** (~10-30 seconds):
   - World generation
   - Arena construction (logged in console)
   - Spawn point set to (0, 65, 0)

3. **Subsequent Starts** (~5 seconds):
   - Arena detected, skipping build
   - Fast world load

## Rebuilding Arena

To regenerate the arena with different settings:

```bash
# Delete world data
./gradlew cleanWorld

# Or manually:
rm -rf server/world

# Re-run setup with different arena type
./setup.sh detailed
```

## Commands

In-game (requires OP):
```
/arena simple     - Rebuild with simple arena
/arena detailed   - Rebuild with detailed arena
/arena rebuild    - Rebuild same type
```

## Troubleshooting

### Java Mismatch

**Symptom**: `[ERROR] Java 21 is required. Detected: 17`

**Solution**: Install JDK 21:
- Ubuntu/Debian: `sudo apt install openjdk-21-jdk`
- macOS: `brew install openjdk@21`
- Windows: Download from [Oracle](https://www.oracle.com/java/technologies/downloads/#java21) or [Adoptium](https://adoptium.net/)

### Paper Download Failure

**Symptom**: Network timeout or 404 error during setup

**Solution**: 
- Check internet connection
- Verify API is accessible:
  ```bash
  curl https://api.papermc.io/v2/projects/paper/versions/1.21.4/builds
  ```
- If behind proxy, configure Gradle proxy settings

### Gradle Wrapper Missing

**Symptom**: `./gradlew: command not found`

**Solution**:
```bash
# If Gradle is installed:
gradle wrapper

# Or commit wrapper files to repository
```

### Plugin Not Loading

**Symptom**: `[ArenaPlugin]` not in server logs

**Solution**:
1. Check plugin JAR exists: `ls plugins/colosseum-arena-1.0.jar`
2. Verify build: `./gradlew jar`
3. Check server logs: `cat server/logs/latest.log | grep Arena`

### Arena Not Building

**Symptom**: Spawn is flat ground, no arena

**Solution**:
```bash
# Delete world and restart
./gradlew cleanWorld
./run.sh
```

### Port Already in Use

**Symptom**: `BindException: Address already in use`

**Solution**:
```bash
# Find process using port 25565
lsof -i :25565
# or
netstat -tulpn | grep 25565

# Kill process or change port in server/server.properties
```

### Out of Memory

**Symptom**: `java.lang.OutOfMemoryError`

**Solution**: JVM is configured for 511MB heap. If insufficient:
1. Edit `build.gradle.kts` (not recommended)
2. Or use external server script with more RAM

## Plugin Extension

To extend the plugin:

1. Edit `src/main/kotlin/com/colosseum/arena/ArenaPlugin.kt`
2. Add commands in `plugin.yml`
3. Rebuild: `./gradlew jar`
4. Copy new JAR: `cp build/libs/*.jar server/plugins/`

## JVM Flags Explanation

The server runs with these optimized flags:

```bash
-Xms511M -Xmx511M              # Fixed 511MB heap (consistent memory)
-XX:+UseG1GC                   # G1 Garbage Collector (low latency)
-XX:+ParallelRefProcEnabled    # Parallel reference processing
-XX:MaxGCPauseMillis=200       # Target 200ms GC pauses
-XX:+UnlockExperimentalVMOptions  # Enable experimental flags
-XX:+DisableExplicitGC         # Ignore System.gc() calls
-XX:G1NewSizePercent=30        # Young gen min 30%
-XX:G1MaxNewSizePercent=40     # Young gen max 40%
-XX:G1HeapRegionSize=8M        # 8MB regions (reduces humongous objects)
-XX:G1ReservePercent=20        # 20% reserve for allocation spikes
-XX:G1HeapWastePercent=5       # Reclaim regions with <5% live data
-Dpaper.disableWatchdog=true   # Disable Paper watchdog (single-threaded build)
-Djava.awt.headless=true       # No GUI dependencies
```

See [JVM Tuning Guide](https://docs.oracle.com/en/java/javase/21/gctuning/) for details.

## Technical Details

### Arena Geometry

- **Center**: X=0, Z=0
- **Ground**: Y=64
- **Spawn**: (0, 65, 0)
- **Wall thickness**: 7.5 blocks (12.0 ≤ r ≤ 19.5)

### Build Process

1. Plugin checks PDC (Persistent Data Container) for `arena_built` key
2. If absent: synchronous block placement (~45k blocks)
3. Sets `arena_built=1` and `arena_type` to prevent rebuild
4. Sets world spawn point

### Dependencies

- Kotlin 2.0.x (compile-only)
- Paper API 1.21.4 (compile-only)
- Zero runtime dependencies

## License

By running this server, you accept the [Minecraft EULA](https://aka.ms/MinecraftEULA).

## Links & Resources

- PaperMC: https://papermc.io
- Paper Docs: https://docs.papermc.io
- Paper API: https://jd.papermc.io/paper/1.21.4/
- Kotlin: https://kotlinlang.org
- JVM Tuning: https://docs.oracle.com/en/java/javase/21/gctuning/
