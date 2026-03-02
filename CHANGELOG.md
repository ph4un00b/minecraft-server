# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[Unreleased]: https://github.com/ph4un00b/minecraft-server/compare/v1.2.0...HEAD
[1.2.0]: https://github.com/ph4un00b/minecraft-server/compare/v1.1.1...v1.2.0
[1.1.1]: https://github.com/ph4un00b/minecraft-server/releases/tag/v1.1.1
