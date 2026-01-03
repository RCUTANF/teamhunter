# Changelog

## Unreleased

## v0.4.4 - 2026-01-03

### Added
- PlayerPositionManager for centralized player position tracking
- Guide system configuration screen with centralized management of various sub-function settings
- ScrollingCarouselText widget for dynamic hint display

### Changed
- Updated to Minecraft 1.21.11 and Gradle 9.2.1
- Moved several configuration screen classes from `.ui` to `.config.screen` package
- Player radar now displays hollow dots for unexposed and unknown enemy entities
- Improved hint descriptions for clarity in JSON configuration files

### Fixed
- Replaced `System.out` with `LOGGER` for improved logging consistency

## v0.4.3 - 2025-12-09

### Added
- Structure data caching and request handling mechanism for StructureTrigger in multiplayer game
- Advancement data caching and request handling functionality for multiplayer game
- Shop title localization support for English and Chinese languages

### Changed
- Refactored GuideSysScreen to separate list data processing logic from rendering concerns
- Centralized GuideSystem data management into dedicated GuideSystemDataManager

### Fixed
- Fixed StructureTrigger failing to detect multiple structures within the same chunk
- Corrected incorrect advancement ID in ThisBoatHasLegsChecker

## v0.4.2 - 2025-12-02

### Added
- support CI/CD.

### Changed
- implement game configuration loading and management system.
