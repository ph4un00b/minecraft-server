# Colosseum Arena

A self-contained, offline-mode PaperMC 1.21.4 Minecraft server with a Kotlin-based plugin that auto-generates a gothic colosseum arena at world spawn.

## Features

- **Zero Configuration**: Runs out of the box
- **Two Arena Types**: Simple or detailed gothic architecture
- **Deterministic Build**: Same arena every time, builds once only
- **Cross-Platform**: Linux, macOS, and Windows support
- **Clean Architecture**: Server files isolated in `server/` folder
- **NPC Combat System**: Configurable Sentinel NPCs for arena battles
- **Combat Kit**: Auto-equip players with enchanted bow and arrows
- **Arrow Tracking**: Persistent arrows with 5 per player limit
- **Async Building**: Lag-free arena rebuilding with progress tracking
- **Command Logging**: Full audit trail of admin commands
- **Spawn Rotation**: Players spawn at E/S/W/N positions cyclically

## Prerequisites

- **Java 21** (JDK required)
- **Gradle** (if wrapper files not committed)
- **Network** (only for initial PaperMC download)

### Required Plugins

This plugin requires these external plugins:
- **Citizens** (NPC framework) - Download from https://wiki.citizensnpcs.co/Versions
- **Sentinel** (NPC combat) - Download from https://wiki.citizensnpcs.co/Sentinel

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

### Local Development (Linux / macOS)

```bash
# Setup (edit templates/phau.properties first to choose arena type)
./scripts/setup.sh

# Start the server
./scripts/start-server.sh
```

### Windows

```cmd
REM Setup (edit templates\phau.properties first to choose arena type)
scripts\setup.bat

REM Start the server
scripts\start-server.bat
```

### Ubuntu Server Deployment

**One-command automated deployment:**

```bash
# Upload deploy.sh to your Ubuntu server, then run:
chmod +x deploy.sh
./deploy.sh --ram=2G --port=25565
```

**Requirements:**
- Ubuntu 20.04/22.04/24.04 LTS
- 2GB+ RAM (4GB recommended)
- Root access

The script automatically installs Java 21, configures firewall, creates systemd service, and starts the server. See [DEPLOY.md](DEPLOY.md) for detailed instructions.

**Note on Architecture:** The deploy script uses a hybrid approach for optimal performance:
- **Setup phase**: Uses Gradle tasks (`./gradlew setup`) to build the plugin and download Paper
- **Runtime**: Uses **direct Java** (not Gradle) to run the server for better performance and lower memory overhead
- This is intentional: Gradle adds ~100-200MB overhead which is significant for 1GB RAM systems

**After deployment, manage with:**
```bash
mcserver start      # Start server
mcserver stop       # Stop server
mcserver logs       # View real-time logs
mcserver backup     # Backup world data
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
├── DEPLOY.md                   # Ubuntu deployment guide
├── build.gradle.kts            # Main build configuration
├── settings.gradle.kts         # Project settings
├── gradlew / gradlew.bat       # Gradle wrapper (in root)
├── scripts/                    # All user scripts
│   ├── setup.sh / setup.bat    # Setup scripts
│   ├── run.sh / run.bat        # Server run scripts (Gradle)
│   ├── start-server.sh / .bat  # Server run scripts (direct Java)
│   ├── fix-locks.sh            # Fix server lock issues
│   └── update-changelog.sh     # Update changelog
├── tasks/                      # Gradle task definitions
├── templates/                  # Configuration templates
│   ├── phau.properties.defaults # Arena config template
│   └── server.properties.defaults # Server config template
├── server/                     # Server files (generated, gitignored)
│   ├── world/                  # World data
│   ├── plugins/                # Plugin JAR + dependencies
│   ├── phau.properties         # Arena config (from template)
│   ├── server.properties       # Server config (from template)
│   └── paper-1.21.4.jar        # Paper server
├── external/                   # Downloaded Paper JAR + plugins
├── plugins/                    # Compiled plugin JAR
└── src/                        # Source code
    ├── main/kotlin/com/colosseum/
    │   ├── arena/
    │   │   ├── ArenaPlugin.kt          # Main plugin class
    │   │   ├── NPCManager.kt           # NPC management
    │   │   ├── NPCConfig.kt            # NPC configuration
    │   │   ├── NPCEvents.kt            # NPC event handlers
    │   │   ├── builders/               # Arena builders
    │   │   │   ├── ArenaBuilder.kt
    │   │   │   ├── SimpleArena.kt
    │   │   │   ├── DetailedArena.kt
    │   │   │   ├── BlockPlacer.kt
    │   │   │   └── QueuedBlockPlacer.kt
    │   │   ├── commands/               # All commands
    │   │   │   ├── ArenaCommand.kt     # Command enum
    │   │   │   ├── BuildCommands.kt
    │   │   │   ├── PlayerCommands.kt
    │   │   │   ├── NPCCommands.kt
    │   │   │   ├── InfoCommands.kt
    │   │   │   ├── CommandLogger.kt
    │   │   │   ├── CommandDisplay.kt
    │   │   │   └── CommandSuggestion.kt
    │   │   ├── combat/                 # Combat system
    │   │   │   ├── CombatKit.kt
    │   │   │   ├── KitConfig.kt
    │   │   │   └── ArrowTracker.kt
    │   │   ├── domain/                 # Domain models
    │   │   │   ├── ArenaConfig.kt
    │   │   │   ├── ArenaType.kt
    │   │   │   ├── NPCAttackType.kt
    │   │   │   ├── SpawnPoint.kt
    │   │   │   └── SpawnPosition.kt
    │   │   ├── manager/                # Arena manager (facade)
    │   │   │   └── ArenaManager.kt
    │   │   └── operations/             # Operations
    │   │       ├── ArenaClearer.kt
    │   │       ├── PlayerSpawner.kt
    │   │       └── YLevelChanger.kt
    │   └── core/storage/
    │       └── PropertiesStorage.kt    # Config storage
    ├── main/resources/
    │   ├── plugin.yml                  # Plugin configuration
    │   └── version.properties          # Build version info
    └── test/kotlin/                    # Unit tests
        └── com/colosseum/arena/commands/
            ├── CommandDisplayTest.kt
            ├── CommandSuggestionTest.kt
            └── CommandCategoriesTest.kt
```

## Gradle Tasks

```bash
# Complete setup
./gradlew setup

# Download PaperMC server
./gradlew downloadPaper

# Download required plugins (Citizens, Sentinel)
./gradlew downloadPlugins

# Force re-download plugins (deletes existing and downloads fresh)
./gradlew downloadPlugins -PforceDownload

# Build plugin only
./gradlew jar

# Run server
./gradlew runServer

# Delete world (regenerate arena)
./gradlew cleanWorld

# Validate Java version
./gradlew checkJava

# Check code style
./gradlew ktlintCheck

# Auto-format code
./gradlew ktlintFormat

# Run tests
./gradlew test

# Full check (tests + lint)
./gradlew check
```

## First Run Expectations

1. **Setup Phase** (~1-2 minutes):
   - Java validation
   - PaperMC download (requires network)
   - Citizens & Sentinel plugin download
   - Plugin compilation
   - Server initialization
   - EULA auto-acceptance

2. **First Server Start** (~10-30 seconds):
   - World generation
   - Arena construction (logged in console)
   - NPC spawning
   - Spawn point markers placed

3. **Subsequent Starts** (~5 seconds):
   - Arena detected, skipping build
   - Fast world load

## Commands

All commands require OP permission (`colosseum.arena.admin`).

### Build Commands
```
/arena simple [f]          - Build simple arena (add 'f' for sync/forced)
/arena detailed [f]        - Build detailed arena (add 'f' for sync/forced)
/arena rebuild [f]         - Rebuild current arena (add 'f' for sync/forced)
/arena sety <level>        - Change arena Y level (requires rebuild)
/arena cancel              - Cancel pending operation
```

### Player Commands
```
/arena restock [player]    - Restock arrows and repair bow
/arena arrows              - Show arrow tracking status
```

### NPC Commands
```
/arena npcs                - Show NPC status
/arena togglenpcs          - Toggle NPCs on/off
/arena setnpccount <0-4>   - Set number of NPCs (max 4)
/arena setnpchealth <hp>   - Set NPC health
/arena setnpcdamage <dmg>  - Set NPC damage
/arena setnpcattack <type> - Set attack type: arrow|fireball
```

### Info Commands
```
/arena spawns              - Show spawn rotation info
/arena version             - Show plugin version
/arena help                - Show command help
```

### Tab Completion
All commands support tab completion for subcommands and arguments.

## Configuration

### Arena Config (`server/phau.properties`)
```properties
# Base Y level where the arena floor is built
arena-base-y=64

# Arena type: simple or detailed
arena-type=detailed
```

### NPC Configuration
NPCs are configured in-memory via commands:
- **Count**: 0-4 NPCs (default: 1)
- **Health**: 1.0+ (default: 20.0)
- **Damage**: 1.0+ (default: 5.0)
- **Attack Type**: `arrow` (spectral) or `fireball` (default: fireball)

### Combat Kit
Players automatically receive:
- Enchanted bow (Power I, Punch I, Infinity)
- 5 arrows (capped at 10 max)
- Arrows persist in world and can be picked up
- Limited to 5 arrows per online player

## Rebuilding Arena

To regenerate the arena with different settings:

```bash
# Delete world data
./gradlew cleanWorld

# Or manually:
rm -rf server/world

# Edit config and restart (arena will regenerate)
nano server/phau.properties  # Change arena-type or arena-base-y
```

## Troubleshooting

### Java Mismatch

**Symptom**: `[ERROR] Java 21 is required. Detected: 17`

**Solution**: Install JDK 21:
- Ubuntu/Debian: `sudo apt install openjdk-21-jdk`
- macOS: `brew install openjdk@21`
- Windows: Download from [Oracle](https://www.oracle.com/java/technologies/downloads/#java21) or [Adoptium](https://adoptium.net/)

### Missing Required Plugins

**Symptom**: `CRITICAL ERROR: Citizens plugin is not installed!`

**Solution**: The server will shut down automatically. Download and install:
1. Citizens from https://wiki.citizensnpcs.co/Versions
2. Sentinel from https://wiki.citizensnpcs.co/Sentinel

Place JARs in `server/plugins/` and restart.

Or use the Gradle task (skips if already exists):
```bash
./gradlew downloadPlugins
```

### Updating Plugin Versions

**Symptom**: Need to update Citizens/Sentinel to newer builds

**Solution**: Force re-download with:
```bash
./gradlew downloadPlugins -PforceDownload
```

This deletes existing plugins and downloads fresh copies.

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
4. Ensure Citizens and Sentinel are installed

### Arena Not Building

**Symptom**: Spawn is flat ground, no arena

**Solution**:
```bash
# Delete world and restart
./gradlew cleanWorld
./scripts/start-server.sh
```

### Port Already in Use

**Symptom**: `BindException: Address already in use`

**Solution**:
```bash
# Quick fix - kills all Java processes and removes lock files
./scripts/fix-locks.sh

# Or manually:
# Find process using port 25565
lsof -i :25565
# Kill the process, then try again
```

### Out of Memory

**Symptom**: `java.lang.OutOfMemoryError`

**Solution**: JVM is configured for 511MB heap. If insufficient:
1. Edit `build.gradle.kts` (not recommended)
2. Or use external server script with more RAM

## Code Formatting

This project uses **ktlint** via the `jlleitschuh/ktlint-gradle` plugin.

```bash
# Check code style (read-only)
./gradlew ktlintCheck

# Auto-format code
./gradlew ktlintFormat

# Full check (tests + lint)
./gradlew check
```

Formatting rules are enforced by ktlint:
- Trailing commas required
- 4-space indentation
- Import ordering
- Max 80 columns (see `.editorconfig`)

## Testing

```bash
# Run all tests
./gradlew test

# Run tests with coverage
./gradlew test jacocoTestReport

# Run tests in specific package
./gradlew test --tests "com.colosseum.arena.*"
```

Tests are located in `src/test/kotlin/` following the package structure.

## Plugin Extension

To extend the plugin:

1. Edit source files in `src/main/kotlin/com/colosseum/arena/`
2. Add commands in `ArenaCommand.kt` enum
3. Implement in appropriate command handler
4. Rebuild: `./gradlew jar`
5. Copy new JAR: `cp build/libs/*.jar server/plugins/`

## Architecture

### Design Principles
- **Composition over inheritance**: Prefer delegation and composition
- **Sealed classes**: Used for type-safe hierarchies
- **Enums over strings**: Type-safe configuration (ArenaType, NPCAttackType)
- **Facade pattern**: ArenaManager delegates to specialized components
- **Fail fast**: Critical errors stop server immediately

### Key Components

**ArenaManager** (`manager/ArenaManager.kt`)
- Facade for all arena operations
- Delegates spawn logic to PlayerSpawner
- Supports sync and async building
- Manages PDC (Persistent Data Container) state

**NPCManager** (`NPCManager.kt`)
- Manages Sentinel NPCs for combat
- Configurable health, damage, count, attack type
- Integrates with Citizens API

**ArrowTracker** (`combat/ArrowTracker.kt`)
- Converts shot arrows to persistent items
- Enforces 5 arrows per player limit
- Items have unlimited lifetime

**Command System** (`commands/`)
- Enum-based command definitions with aliases
- Categorized handlers (Build, Player, NPC, Info)
- Command logging for audit trail
- Tab completion support

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
- **Ground**: Y=64 (configurable via `arena-base-y`)
- **Spawn**: (0, 65, 0)
- **Wall thickness**: 7.5 blocks (12.0 ≤ r ≤ 19.5)
- **NPC spawn radius**: 6 blocks from center
- **Player spawn positions**: E (0°, r=8), S (90°, r=8), W (180°, r=8), N (270°, r=8)

### Build Process

1. Plugin checks PDC for `arena_built` key
2. If absent: blocks are queued and placed
3. Synchronous: immediate placement (may cause lag)
4. Asynchronous: 100 blocks per tick (lag-free)
5. Spawn markers built at cardinal points
6. NPCs spawned at positions around center
7. Sets `arena_built=1` and `arena_type` to prevent rebuild
8. Sets world spawn point

### Dependencies

- **Kotlin** 2.0.x (compile-only)
- **Paper API** 1.21.11-R0.1-SNAPSHOT (compile-only)
- **Citizens** 2.0.37-SNAPSHOT (runtime required)
- **Sentinel** 2.9.3-SNAPSHOT (runtime required)
- **Kotlin stdlib** (shaded in JAR)
- **JUnit** 5.10.0 (test only)

### Version Management

Version info stored in `src/main/resources/version.properties`:
- Auto-updates during build with git info
- Format: `1.0+<git-short-hash>`
- Includes build timestamp and git branch
- Use `VersionInfo.load(plugin)` to access

## Git Workflow

This project follows **GitHub Flow** with `--no-ff` merges.

### Branch Naming
- `main` - Production-ready code (never commit directly)
- `feat/*` - New features (e.g., `feat/npc-fireballs`)
- `fix/*` - Bug fixes (e.g., `fix/spawn-location`)
- `experiment/*` - Testing ideas (delete when done)

### Commit Message Format
```
type(scope): description

Types: feat, fix, refactor, test, docs, chore
Example: feat(npcs): add configurable fireball attack type
```

## License

By running this server, you accept the [Minecraft EULA](https://aka.ms/MinecraftEULA).

## Links & Resources

- PaperMC: https://papermc.io
- Paper Docs: https://docs.papermc.io
- Paper API: https://jd.papermc.io/paper/1.21.4/
- Citizens: https://wiki.citizensnpcs.co/
- Sentinel: https://wiki.citizensnpcs.co/Sentinel
- Kotlin: https://kotlinlang.org
- JVM Tuning: https://docs.oracle.com/en/java/javase/21/gctuning/
