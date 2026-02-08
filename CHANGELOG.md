# Changelog

All notable changes to VoidStream will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- CONTRIBUTING.md with comprehensive contribution guidelines
- CODE_OF_CONDUCT.md based on Contributor Covenant v2.1
- Issue templates for bug reports and feature requests
- Pull request template for consistent PR structure
- This CHANGELOG.md file

## [2.0.1] - 2026-02-08

### Added

- Google Play Store distribution flavor (`googleplay`)
- Security policy (SECURITY.md) with vulnerability disclosure process
- Separate `IS_GOOGLE_PLAY_BUILD` flag for Google Play-specific gating
- AAB (Android App Bundle) build support for Google Play submission

### Changed

- Transitioned to GPL v2 open source license
- Split store-specific flags: `IS_AMAZON_BUILD` and `IS_GOOGLE_PLAY_BUILD`
- Rebranded from "Voidfin" to "VoidStream"
- Updated README with GPL v2 license section and plugin architecture details

### Fixed

- OTA update checks now properly skip on both Amazon and Google Play builds
- Conditional DI injection for flavor-gated services (prevents crashes on excluded flavors)

## [1.7.0] - 2026-01-15

### Added

- Google Play build flavor with separate manifest
- Version badge automation workflow in CI/CD
- Comprehensive README badges for SDK versions, Kotlin, Java, Gradle

### Changed

- Updated README with supported devices and platforms section
- Enhanced CI/CD pipeline with multiple workflows

## [1.6.12] - 2025-12-10

### Added

- Initial fork from Jellyfin Android TV
- VoidStream branding (logos, app name, visual assets)
- OTA update system for GitHub releases
- Forced update capability with `[FORCE]` tag in release notes
- "What's New" dialog after updates
- Custom IPTV integration (initial foundation)

### Changed

- Application ID changed to `org.voidstream.androidtv`
- Updated all branding assets and strings

## [1.6.0] - 2025-11-20

### Changed

- Forked from Jellyfin Android TV
- Initial repository setup

---

## Version Links

[Unreleased]: https://github.com/ToastyToast25/VoidStream-FireTV/compare/v2.0.1...HEAD
[2.0.1]: https://github.com/ToastyToast25/VoidStream-FireTV/releases/tag/v2.0.1
[1.7.0]: https://github.com/ToastyToast25/VoidStream-FireTV/releases/tag/v1.7.0
[1.6.12]: https://github.com/ToastyToast25/VoidStream-FireTV/releases/tag/v1.6.12
[1.6.0]: https://github.com/ToastyToast25/VoidStream-FireTV/releases/tag/v1.6.0

---

## Categories

This changelog uses the following categories:

- **Added** - New features
- **Changed** - Changes to existing functionality
- **Deprecated** - Soon-to-be removed features
- **Removed** - Removed features
- **Fixed** - Bug fixes
- **Security** - Vulnerability fixes

## Maintenance

When releasing a new version:

1. Move items from `[Unreleased]` to the new version section
2. Update the version date
3. Add version link at the bottom
4. Update the `[Unreleased]` comparison link
5. Commit with message: `docs: update CHANGELOG for vX.Y.Z`
