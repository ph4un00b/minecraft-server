# AGENTS.md

This file contains guidelines and commands for agentic coding agents working in this repository.

## Project Overview

This is a Kotlin-based Minecraft PaperMC plugin that auto-generates gothic colosseum arenas. The project uses Gradle for building and follows Kotlin conventions with Java 21 toolchain.


## Code Style Guidelines

- always ask for clarification never assume
- avoid inheritance
- always ask me when boolean are gonna be used, maybe en enumerator is better
- prefer enums over strings when apropiate
- prefer sealed classes
- composition over inheritance
- arena-manager is mostly a facede and uses delegation for other behaviors

### Error Handling
- Use try-catch blocks for initialization failures
- Log errors with appropriate severity (`logger.severe`, `logger.warning`)
- Fail fast in `onEnable()` if critical components fail
- Use meaningful error messages with context

### Version Management
- Version info stored in `version.properties`
- Auto-update during build with git info
- Include version in JAR manifest
- Use `VersionInfo.load(plugin)` to access version data

## Testing Guidelines

- always ask me if mocking is gonna be used
- never hardcode values in order to pass tests

### Test Structure
- Test classes in `src/test/kotlin/` following package structure
- Use descriptive test method names
- Test both happy path and error conditions

### Test Commands
```bash
# Run all tests
./gradlew test

# Run tests with coverage
./gradlew test jacocoTestReport

# Run tests in specific package
./gradlew test --tests "com.colosseum.arena.*"
```

## Code Formatting

This project uses **ktlint** via the `jlleitschuh/ktlint-gradle` plugin for Kotlin code formatting.

### Format Commands
```bash
# Check code style (read-only, reports violations)
./gradlew ktlintCheck

# Auto-format code (fixes issues where possible)
./gradlew ktlintFormat

# Check runs both tests and ktlintCheck
./gradlew check
```

### Formatting Guidelines
- Run `./gradlew ktlintFormat` before committing to ensure consistent code style
- The `check` task includes ktlint validation - CI will fail on style violations
- Formatting rules are enforced by ktlint (trailing commas, indentation, import ordering, etc.)
- Maximum line width is 80 columns - configure in `.editorconfig`

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
