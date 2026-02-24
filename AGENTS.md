# AGENTS.md

This file contains guidelines and commands for agentic coding agents working in this repository.

## Project Overview

This is a Kotlin-based Minecraft PaperMC plugin that auto-generates gothic colosseum arenas. The project uses Gradle for building and follows Kotlin conventions with Java 21 toolchain.

## Build Commands

### Core Commands
```bash
# Complete setup (recommended for new agents)
./gradlew setup

# Build plugin only
./gradlew jar

# Run server (for testing)
./gradlew runServer

# Validate Java version
./gradlew checkJava

# Download PaperMC server
./gradlew downloadPaper

# Delete world (regenerate arena)
./gradlew cleanWorld
```

### Development Workflow
```bash
# Build and copy to server
./gradlew jar && cp build/libs/*.jar server/plugins/

# Clean and rebuild
./gradlew clean && ./gradlew jar
```

### Testing
This project uses JUnit 5 for testing. Run tests with:
```bash
./gradlew test

# Run specific test class
./gradlew test --tests "com.colosseum.arena.specific.TestClass"

# Run specific test method
./gradlew test --tests "com.colosseum.arena.specific.TestClass.testMethod"
```

## Code Style Guidelines

- prefer enums over strings when apropiate
- prefer sealed classes
- composition over inheritance
- arena-manager is mostly a facede and uses delegation for other behaviors

### Kotlin Conventions
- **Target**: JVM 21
- **Warnings**: All warnings as errors (`-Werror`)
- **Linting**: Full linting enabled
- **Formatting**: Follow Kotlin standard conventions
- **Null Safety**: Use nullable types (`?`) and non-null assertions (`!!`) appropriately

### Import Organization
```kotlin
// 1. Kotlin standard library
import kotlin.*

// 2. Java standard library
import java.*

// 3. Third-party libraries
import org.bukkit.*
import com.colosseum.*
```

### Naming Conventions
- **Classes**: PascalCase (e.g., `ArenaPlugin`, `ArenaManager`)
- **Functions**: camelCase (e.g., `checkAndBuild`, `initializeComponents`)
- **Variables**: camelCase (e.g., `versionInfo`, `arrowTracker`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_ARROWS`)
- **Packages**: lowercase with dots (e.g., `com.colosseum.arena`)

### File Structure
```
src/main/kotlin/com/colosseum/
├── arena/           # Core arena logic
├── combat/          # Combat-related classes
├── builders/        # Arena builders (SimpleArena, DetailedArena)
├── domain/          # Domain models (ArenaConfig, SpawnPoint)
├── operations/      # Arena operations (ArenaClearer, YLevelChanger)
├── manager/         # ArenaManager facade
└── core/storage/    # Storage utilities
```

### Error Handling
- Use try-catch blocks for initialization failures
- Log errors with appropriate severity (`logger.severe`, `logger.warning`)
- Fail fast in `onEnable()` if critical components fail
- Use meaningful error messages with context

### Logging Conventions
```kotlin
// Use prefix for all plugin logs
private val prefix = "\u001B[32m[ArenaPlugin]\u001B[0m "

// Log levels
logger.info("$prefix Message")      // General info
logger.warning("$prefix Warning")   // Warnings
logger.severe("$prefix Error")      // Critical errors
```

### Data Classes
- Use data classes for simple data holders
- Include companion objects for factory methods when needed
- Follow the pattern in `VersionInfo` data class

### Plugin Architecture
- **Main Class**: Extends `JavaPlugin`, implements `Listener`
- **Components**: Initialize in `initializeComponents()` method
- **Manager Pattern**: Use `ArenaManager` as facade for complex operations
- **Event Handling**: Register events in `onEnable()`, unregister in `onDisable()`

### Command Structure
- Commands in `onCommand()` method
- Permission checking first
- Help message for empty arguments
- Use when expressions for command routing
- Return `true` for handled commands, `false` for unhandled

### Arena Building
- Check `arena_built` PDC key before building
- Use synchronous block placement for arena construction
- Set spawn point after arena build
- Log build progress and completion

### Version Management
- Version info stored in `version.properties`
- Auto-update during build with git info
- Include version in JAR manifest
- Use `VersionInfo.load(plugin)` to access version data

## Testing Guidelines

### Test Structure
- Test classes in `src/test/kotlin/` following package structure
- Use descriptive test method names
- Test both happy path and error conditions
- Mock Bukkit API where necessary

### Test Commands
```bash
# Run all tests
./gradlew test

# Run tests with coverage
./gradlew test jacocoTestReport

# Run tests in specific package
./gradlew test --tests "com.colosseum.arena.*"
```

## Security Notes

- Never commit secrets or API keys
- Validate all user inputs in commands
- Use proper permission checks for administrative commands
- Follow Minecraft EULA requirements
