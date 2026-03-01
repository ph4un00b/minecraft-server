# AGENTS.md

This file contains guidelines and commands for agentic coding agents working in this repository.

## Project Overview

This is a Kotlin-based Minecraft PaperMC plugin that auto-generates gothic colosseum arenas. The project uses Gradle for building and follows Kotlin conventions with Java 21 toolchain.



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



### Naming Conventions
- **Classes**: PascalCase (e.g., `ArenaPlugin`, `ArenaManager`)
- **Functions**: camelCase (e.g., `checkAndBuild`, `initializeComponents`)
- **Variables**: camelCase (e.g., `versionInfo`, `arrowTracker`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_ARROWS`)
- **Packages**: lowercase with dots (e.g., `com.colosseum.arena`)


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

## Git Workflow

This project follows **GitHub Flow** with `--no-ff` merges for easy rollbacks.

### Branch Naming
- `main` - Production-ready code (never commit directly)
- `feat/*` - New features (e.g., `feat/npc-fireballs`)
- `fix/*` - Bug fixes (e.g., `fix/spawn-location`)
- `experiment/*` - Testing ideas (delete when done)

### Development Workflow

```bash
# 1. Start new feature
git checkout main
git pull origin main
git checkout -b feat/description

# 2. Work and commit
git add .
git commit -m "feat(scope): description"

# 3. Test before merging
./gradlew test
./gradlew jar && ./gradlew runServer

# 4. Merge with --no-ff (creates merge commit for easy revert)
git checkout main
git merge --no-ff feat/description
```

### Commit Message Format
```
type(scope): description

Types: feat, fix, refactor, test, docs, chore
Example: feat(npcs): add configurable fireball attack type
```

### Emergency Revert
```bash
# Find merge commit hash
git log --oneline --merges

# Revert entire feature
git revert <merge-commit-hash>
```

### Release Tagging
```bash
git tag -a v1.2.0 -m "Add NPC combat system"
```

## Security Notes

- Never commit secrets or API keys
- Validate all user inputs in commands
- Use proper permission checks for administrative commands
- Follow Minecraft EULA requirements
