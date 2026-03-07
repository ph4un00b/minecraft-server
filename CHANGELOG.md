# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.3.0] - 2026-03-07

### Added
- **NPC Combat Enhancements**:
  - New FIREBALL attack type (20% probability) with blaze rod equipment
  - Dynamic command validation using enum's `validCommandNames` property
  - Victory sound (ENTITY_PLAYER_LEVELUP) when batch is cleared
  - Title screen notification "BATCH #N CLEARED!" with next batch size
  - Action bar message "Target hit! NPCs are now HOSTILE!" on target activation
- **ArenaMessage System**: New sealed class for broadcasting messages to all players
  - `BatchCleared` title message with batch number and next enemy count
  - `TargetActivated` action bar message for target block hits
  - `broadcast(world)` method to send to all players in arena world
- **NPCManager.reset()**: Single method to reset all NPC state for arena rebuilds
  - Clears NPCs, resets batch index, restores starting batch size
  - Clears spawn tracking and resets target block listener
- **Regression Tests**: BatchIndexTest to prevent batch tracking bugs
- Dragon growl sound (ENTITY_ENDER_DRAGON_GROWL) when target block is hit

### Changed
- **Major Refactoring**: Restructured entire codebase with feature-based package organization
  - Moved combat files to `com.colosseum.combat.*` (kit, arrow, spawn)
  - Moved NPC files to `com.colosseum.npc.*` (manager, config, decisions, attack types)
  - Moved command files to `com.colosseum.commands.*` (handler and infrastructure)
  - Moved target block files to `com.colosseum.target.*`
  - Moved ArenaManager to `com.colosseum.arena` (root level as facade)
  - Renamed `builders/` to `builder/` (singular)
- Restructured test files to mirror new package layout
  - Tests now in matching packages: `npc/`, `commands/`
  - Moved test resources to `src/test/resources/`
- Updated all documentation (README.md, AGENTS.md) with new structure
- Arena rebuilds now use `npcManager.reset()` instead of multiple separate calls
- NPC probability distribution adjusted (existing types scaled to 80% to accommodate fireball)

### Fixed
- Import ordering across all files (ktlint compliance)
- Test resource path in CommandDisplayTest
- **Batch Index Bug**: Fixed issue where batch number always showed as #1
  - Separated `clearAllNPCs()` from `resetBatchIndex()` concerns
  - Batch index now properly increments between waves
  - Only resets to 1 on full arena rebuilds or manual NPC count changes

## [1.2.0] - 2026-03-01

### Added
- Snapshot testing for command display output validation
- `expected_commands_display.txt` reference file for command output testing
- Visual width calculation for proper emoji rendering in box displays
- ktlint integration with 80-column maximum line length enforcement
- `.editorconfig` with project formatting standards

### Changed
- Refactored `CommandDisplay` with emoji-aware formatting
- Consolidated 12 separate display tests into 2 focused snapshot tests
- Updated code style guidelines in AGENTS.md
- Improved box formatting consistency in command displays

### Fixed
- Fixed box spacing in command display header
- Fixed footer message to include proper attribution

## [1.1.1] - Previous release

### Added
- Initial release with arena generation commands
- NPC management system
- Player combat kit and arrow tracking
- Command categorization and help system

[Unreleased]: https://github.com/ph4un00b/minecraft-server/compare/v1.3.0...HEAD
[1.3.0]: https://github.com/ph4un00b/minecraft-server/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/ph4un00b/minecraft-server/compare/v1.1.1...v1.2.0
[1.1.1]: https://github.com/ph4un00b/minecraft-server/releases/tag/v1.1.1
