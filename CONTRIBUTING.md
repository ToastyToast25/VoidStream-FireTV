# Contributing to VoidStream

Thank you for your interest in contributing to VoidStream! We welcome contributions from the community and appreciate your help in making this project better.

Please take a moment to review this document before submitting your contribution. Following these guidelines helps us process your contributions more efficiently and ensures a positive experience for everyone involved.

## Code of Conduct

This project and everyone participating in it is governed by our [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to haydenrmccray@gmail.com.

## Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

- **JDK 17** or higher (OpenJDK or Oracle JDK)
- **Android Studio Hedgehog (2023.1.1)** or later
- **Git** for version control
- **Android SDK 34** (installed via Android Studio)

Optional but recommended:
- **ktlint** for code formatting
- **bundletool** for AAB testing
- **GitHub CLI (gh)** for release management

### Forking and Cloning

1. Fork the repository on GitHub by clicking the "Fork" button
2. Clone your fork locally:

```bash
git clone https://github.com/YOUR_USERNAME/VoidStream-FireTV.git
cd VoidStream-FireTV
```

3. Add the upstream repository as a remote:

```bash
git remote add upstream https://github.com/ToastyToast25/VoidStream-FireTV.git
```

### Building from Source

VoidStream has **3 build flavors** for different distribution channels:

```bash
# GitHub flavor (sideloaded, includes OTA updates)
./gradlew assembleGithubDebug

# Amazon Appstore flavor (no OTA, store-compliant)
./gradlew assembleAmazonDebug

# Google Play flavor (no OTA, store-compliant)
./gradlew assembleGoogleplayDebug
```

For detailed build instructions, see [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).

### Running Tests

Before submitting your contribution, run the test suite:

```bash
# Run all unit tests
./gradlew test

# Run lint checks
./gradlew lint

# Run ktlint (Kotlin code style)
./gradlew ktlintCheck

# Run all checks (tests + lint + ktlint)
./gradlew check
```

## Development Workflow

1. **Create a feature branch** from `master`:
   ```bash
   git checkout -b feature/my-awesome-feature
   ```

2. **Make your changes** following our code style guidelines (see below)

3. **Write or update tests** for your changes:
   - Unit tests for business logic
   - UI tests for user interface changes
   - All new features should have tests

4. **Run local checks** to ensure your changes pass:
   ```bash
   ./gradlew check
   ```

5. **Commit your changes** using [Conventional Commits](https://www.conventionalcommits.org/):
   ```bash
   git commit -m "feat: add support for custom subtitles"
   git commit -m "fix: resolve crash on Fire TV Stick"
   git commit -m "docs: update README with new features"
   ```

6. **Push to your fork**:
   ```bash
   git push origin feature/my-awesome-feature
   ```

7. **Open a pull request** on GitHub and fill out the PR template

## Code Style

### Kotlin Conventions

- Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use **4 spaces** for indentation (no tabs)
- Max line length: **120 characters**
- Use **ktlint** for automatic formatting: `./gradlew ktlintFormat`

### Naming Conventions

- Classes: `PascalCase` (e.g., `UpdateCheckerService`)
- Functions: `camelCase` (e.g., `checkForUpdates()`)
- Constants: `SCREAMING_SNAKE_CASE` (e.g., `MAX_RETRY_COUNT`)
- Resources: `snake_case` (e.g., `ic_voidstream.png`)

### Code Organization

- Follow existing package structure in the codebase
- Keep classes focused and single-purpose
- Use dependency injection (Koin) for service dependencies
- Avoid hardcoded values; use constants or resources

## Pull Request Process

1. **Fill out the PR template** completely:
   - Describe what your PR changes and why
   - Link to related issues (e.g., "Fixes #123")
   - List which build flavors are affected
   - Include screenshots/videos for UI changes

2. **Ensure all CI checks pass**:
   - Build must succeed for all flavors
   - All tests must pass
   - Lint checks must pass
   - No new warnings introduced

3. **Wait for code review**:
   - All PRs require at least 1 approval from a maintainer
   - Address any feedback or requested changes
   - Keep the conversation respectful and constructive

4. **Squash and merge**:
   - Once approved, a maintainer will merge your PR
   - Your commits will be squashed into a single commit

## Issue Reporting

Before opening a new issue, please:

1. **Search existing issues** to avoid duplicates
2. **Use issue templates** - they help us gather necessary information
3. **Provide reproduction steps** for bugs
4. **Include environment details** (device, Android version, app version)

### Issue Templates

We have templates for:
- **Bug Report** - Report a crash or unexpected behavior
- **Feature Request** - Suggest a new feature or enhancement
- **Security Vulnerability** - Report security issues privately via [SECURITY.md](SECURITY.md)

## Build Flavors and Feature Gating

VoidStream uses Gradle product flavors to support different distribution channels:

| Flavor | Purpose | OTA Updates | Install Permission |
|--------|---------|-------------|-------------------|
| `github` | Sideloaded (GitHub Releases) | ✅ Yes | ✅ Yes |
| `amazon` | Amazon Appstore | ❌ No | ❌ No |
| `googleplay` | Google Play Store | ❌ No | ❌ No |

### Adding Store-Gated Features

If your feature should only be available on certain flavors:

1. Use `BuildConfig.IS_AMAZON_BUILD` or `BuildConfig.IS_GOOGLE_PLAY_BUILD` flags
2. For sideload-only features: `if (!IS_AMAZON_BUILD && !IS_GOOGLE_PLAY_BUILD) { ... }`
3. If adding a dependency injection bean, gate it in `AppModule.kt`
4. Use safe-call operators (`?.`) when accessing gated services
5. Test your changes on **all 3 flavors**

Example:
```kotlin
// In AppModule.kt
if (!BuildConfig.IS_AMAZON_BUILD && !BuildConfig.IS_GOOGLE_PLAY_BUILD) {
    single { UpdateCheckerService(get(), get()) }
}

// In usage
private val updateChecker: UpdateCheckerService? by lazy {
    if (!IS_AMAZON_BUILD && !IS_GOOGLE_PLAY_BUILD) get() else null
}

// Safe usage
updateChecker?.checkForUpdates()
```

## Plugin Development

VoidStream supports **separate plugin modules** that extend functionality beyond the core client.

### Plugin Architecture

- **Core client**: GPL v2 licensed (open source)
- **Plugins**: Can be proprietary and monetized
- **Communication**: Plugins are separate APKs that communicate via IPC (AIDL)
- **First plugin**: IPTV plugin (M3U, Xtream Codes, Stalker) - coming soon

### Contributing Plugin Code

- **Do NOT** add plugin code to the core repository
- Plugin API documentation is under development
- Contact haydenrmccray@gmail.com for plugin development inquiries

## Commit Message Guidelines

We use [Conventional Commits](https://www.conventionalcommits.org/) for clear and structured commit messages:

- `feat:` - A new feature
- `fix:` - A bug fix
- `docs:` - Documentation changes
- `style:` - Code style changes (formatting, no code change)
- `refactor:` - Code refactoring (no functional change)
- `test:` - Adding or updating tests
- `chore:` - Maintenance tasks (dependencies, build config)
- `perf:` - Performance improvements
- `ci:` - CI/CD changes

Examples:
```
feat: add voice search support for Android TV
fix: resolve crash when network is unavailable
docs: update CONTRIBUTING.md with plugin guidelines
refactor: extract playback logic to separate module
test: add unit tests for UpdateCheckerService
```

## License

By contributing to VoidStream, you agree that your contributions will be licensed under the **GPL v2 license**.

### Important Notes

- All contributions to the core client become GPL v2
- You **cannot** contribute proprietary code to the core repository
- If you want to build a proprietary extension, create a separate plugin
- VoidStream's plugin architecture keeps GPL and proprietary code separate

See [LICENSE](LICENSE) for full license text.

## Attribution

Contributors will be acknowledged in:
- Git commit history
- Release notes (for significant contributions)
- GitHub's contributor graph

## Getting Help

Need help with your contribution?

- **GitHub Discussions** - Ask questions, discuss ideas
- **Documentation** - See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for detailed setup
- **Issue Tracker** - Report problems or request features
- **Email** - haydenrmccray@gmail.com for private inquiries

## What to Contribute

Not sure where to start? Here are some ideas:

- **Fix bugs** - Check issues labeled `bug` or `good-first-issue`
- **Add tests** - Improve test coverage
- **Improve documentation** - Fix typos, clarify instructions
- **Optimize performance** - Profile and improve slow code
- **Add translations** - Help translate the app to other languages
- **Review PRs** - Provide feedback on open pull requests

Thank you for contributing to VoidStream! 🚀
