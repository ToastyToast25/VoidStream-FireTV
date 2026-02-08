# Development Setup Guide

This guide will help you set up your development environment and start contributing to VoidStream.

## Prerequisites

### Required Software

1. **JDK 17 or higher**
   - [Download OpenJDK 17](https://adoptium.net/) (recommended)
   - Or Oracle JDK 17+
   - Verify: `java -version` should show 17 or higher

2. **Android Studio Hedgehog (2023.1.1) or later**
   - [Download Android Studio](https://developer.android.com/studio)
   - Includes Android SDK, emulator, and Gradle

3. **Git for version control**
   - Windows: [Git for Windows](https://git-scm.com/download/win)
   - Mac: `brew install git` or Xcode Command Line Tools
   - Linux: `sudo apt install git` (Ubuntu/Debian)
   - Verify: `git --version`

4. **Android SDK 34** (API Level 34 - Android 14)
   - Installed automatically via Android Studio
   - Or manually via Android Studio → SDK Manager

### Optional Tools

- **ktlint** - Kotlin code style formatter
  ```bash
  # Via Gradle (recommended)
  ./gradlew ktlintFormat

  # Or install globally
  brew install ktlint  # Mac
  ```

- **bundletool** - Test AAB files locally
  ```bash
  # Download from https://github.com/google/bundletool
  # Or via Android Studio SDK Manager → Command-line Tools
  ```

- **GitHub CLI (gh)** - Release management
  ```bash
  # Mac
  brew install gh

  # Windows
  winget install GitHub.cli

  # Linux
  sudo apt install gh
  ```

## Getting Started

### 1. Fork and Clone the Repository

**Fork on GitHub:**
1. Go to https://github.com/ToastyToast25/VoidStream-FireTV
2. Click "Fork" button (top right)
3. Select your GitHub account

**Clone your fork:**
```bash
git clone https://github.com/YOUR_USERNAME/VoidStream-FireTV.git
cd VoidStream-FireTV
```

**Add upstream remote:**
```bash
git remote add upstream https://github.com/ToastyToast25/VoidStream-FireTV.git

# Verify remotes
git remote -v
# origin    https://github.com/YOUR_USERNAME/VoidStream-FireTV.git (fetch)
# origin    https://github.com/YOUR_USERNAME/VoidStream-FireTV.git (push)
# upstream  https://github.com/ToastyToast25/VoidStream-FireTV.git (fetch)
# upstream  https://github.com/ToastyToast25/VoidStream-FireTV.git (push)
```

### 2. Configure Signing (Release Builds Only)

**For debug builds:** Skip this step (debug builds are auto-signed)

**For release builds:**

Create `keystore.properties` in project root:
```properties
storeFile=release.keystore
storePassword=your_store_password
keyAlias=voidstream
keyPassword=your_key_password
```

**Note:** Use `keystore.properties.template` as reference. **Never commit `keystore.properties`** (it's git-ignored).

### 3. Open Project in Android Studio

1. Launch Android Studio
2. **File → Open** → Select `VoidStream-FireTV` folder
3. Wait for Gradle sync (first time may take 5-10 minutes)
4. Android Studio will download dependencies automatically

**If Gradle sync fails:**
```bash
# Command line sync
./gradlew build --refresh-dependencies
```

## Building the App

VoidStream has **3 build flavors** for different distribution channels. Each flavor has debug and release variants.

### Debug Builds (Unsigned, for Development)

**GitHub flavor** (includes OTA updates):
```bash
./gradlew assembleGithubDebug
```
Output: `app/build/outputs/apk/github/debug/voidstream-androidtv-v*.apk`

**Amazon Appstore flavor** (no OTA, store-compliant):
```bash
./gradlew assembleAmazonDebug
```
Output: `app/build/outputs/apk/amazon/debug/voidstream-androidtv-v*.apk`

**Google Play flavor** (no OTA, store-compliant):
```bash
./gradlew assembleGoogleplayDebug
```
Output: `app/build/outputs/apk/googleplay/debug/voidstream-androidtv-v*.apk`

**Build all flavors at once:**
```bash
./gradlew assembleDebug
```

### Release Builds (Signed, for Production)

**Requires `keystore.properties` configured (see section 2 above)**

**GitHub release** (APK for sideloading):
```bash
./gradlew assembleGithubRelease
```
Output: `app/build/outputs/apk/github/release/voidstream-androidtv-v*-github-release.apk`

**Amazon release** (APK for Amazon Appstore):
```bash
./gradlew assembleAmazonRelease
```
Output: `app/build/outputs/apk/amazon/release/voidstream-androidtv-v*-amazon-release.apk`

**Google Play AAB** (required for Google Play Store):
```bash
./gradlew bundleGoogleplayRelease
```
Output: `app/build/outputs/bundle/googleplayRelease/voidstream-androidtv-v*-googleplay-release.aab`

**Build all release variants:**
```bash
./gradlew assembleRelease
```

## Running the App

### Install on Device/Emulator

**Via command line:**
```bash
# Install GitHub debug build
adb install -r app/build/outputs/apk/github/debug/voidstream-androidtv-v*-github-debug.apk

# Launch app
adb shell monkey -p org.voidstream.androidtv.debug 1
```

**Via Android Studio:**
1. Select run configuration (e.g., "app" with "github" flavor)
2. Click Run ▶️ or press `Shift+F10`
3. Select target device (emulator or physical device)

**Package names by build type:**
- **Debug:** `org.voidstream.androidtv.debug`
- **Release:** `org.voidstream.androidtv`

### Emulator Setup

**Create Android TV emulator:**

1. Android Studio → Device Manager (or Tools → Device Manager)
2. Click **Create Device**
3. Select **TV** category
4. Choose **1080p (Android TV)** or **4K (Android TV)**
5. Select system image: **API 33** (Android 13) or higher
6. Click **Finish**

**Configure emulator:**
- **RAM:** 2048 MB minimum (4096 MB recommended)
- **Internal storage:** 2048 MB minimum
- **Graphics:** Hardware - GLES 2.0

**Start emulator:**
```bash
# List available emulators
emulator -list-avds

# Start specific emulator
emulator -avd Android_TV_1080p_API_33 &
```

**Fire TV differences:**
- Fire OS is based on Android, so Android TV emulator works for most testing
- Fire TV Stick 4K recommended for final testing (physical device)

### Physical Device Setup

**Enable Developer Options:**
1. Settings → Device Preferences → About
2. Tap "Build" 7 times
3. Go back → Developer Options → Enable **USB debugging**

**Connect via USB:**
```bash
# Connect device via USB cable
adb devices
# List of devices attached
# 1234567890ABCDEF        device
```

**Connect via Wi-Fi** (easier for TV devices):
```bash
# On device: Settings → Developer Options → Network Debugging → Enable
# Note the IP address (e.g., 192.168.1.100)

# On computer:
adb connect 192.168.1.100:5555
adb devices
# 192.168.1.100:5555      device
```

## Testing

### Unit Tests

**Run all unit tests:**
```bash
./gradlew test
```

**Run tests for specific module:**
```bash
./gradlew app:test
./gradlew playback:test
```

**Run tests for specific flavor:**
```bash
./gradlew testGithubDebugUnitTest
```

**Generate coverage report:**
```bash
./gradlew jacocoTestReport
# Report: app/build/reports/jacoco/test/html/index.html
```

**Run specific test class:**
```bash
./gradlew test --tests UpdateCheckerServiceTest
```

**Test reports:**
- HTML: `app/build/reports/tests/testGithubDebugUnitTest/index.html`
- XML: `app/build/test-results/testGithubDebugUnitTest/`

### UI Tests (Instrumented)

**Run on connected device/emulator:**
```bash
# Run all instrumented tests
./gradlew connectedAndroidTest

# Run specific flavor
./gradlew connectedGithubDebugAndroidTest
```

**Run specific test class:**
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.jellyfin.androidtv.ui.HomeFragmentTest
```

**Test reports:**
- HTML: `app/build/reports/androidTests/connected/index.html`

### Lint Checks

**Run Android lint:**
```bash
./gradlew lint
# Report: app/build/reports/lint-results-githubDebug.html
```

**Run ktlint** (Kotlin code style):
```bash
# Check style violations
./gradlew ktlintCheck

# Auto-fix violations
./gradlew ktlintFormat
```

**Run all checks** (tests + lint + ktlint):
```bash
./gradlew check
```

## Code Style

### Kotlin Conventions

Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

- **Indentation:** 4 spaces (no tabs)
- **Max line length:** 120 characters
- **Naming:**
  - Classes: `PascalCase` (e.g., `UpdateCheckerService`)
  - Functions: `camelCase` (e.g., `checkForUpdates()`)
  - Constants: `SCREAMING_SNAKE_CASE` (e.g., `MAX_RETRY_COUNT`)
  - Resources: `snake_case` (e.g., `ic_voidstream.png`)

**Auto-format code:**
```bash
# Format all Kotlin files
./gradlew ktlintFormat

# Or in Android Studio: Code → Reformat Code (Ctrl+Alt+L)
```

### Commit Message Guidelines

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>: <description>

[optional body]

[optional footer]
```

**Types:**
- `feat:` - A new feature
- `fix:` - A bug fix
- `docs:` - Documentation changes
- `style:` - Code style changes (formatting, no logic change)
- `refactor:` - Code refactoring (no functional change)
- `test:` - Adding or updating tests
- `chore:` - Maintenance tasks (dependencies, build)
- `perf:` - Performance improvements
- `ci:` - CI/CD configuration changes

**Examples:**
```bash
git commit -m "feat: add voice search support for Android TV"
git commit -m "fix: resolve crash when network is unavailable"
git commit -m "docs: update CONTRIBUTING.md with new guidelines"
git commit -m "refactor: extract playback logic to separate module"
git commit -m "test: add unit tests for UpdateCheckerService"
```

## Debugging

### Logcat Filtering

**Filter by app package:**
```bash
adb logcat | grep org.voidstream.androidtv
```

**Filter by custom tag:**
```bash
adb logcat -s "VoidStream"
```

**Filter multiple tags:**
```bash
adb logcat VoidStream:D AndroidRuntime:E *:S
```

**Clear logcat:**
```bash
adb logcat -c
```

**Save logcat to file:**
```bash
adb logcat > logcat.txt
```

### Android Studio Debugger

**Set breakpoints:**
- Click left margin next to line number
- Or `Ctrl+F8` on the line

**Start debugging:**
- Click Debug 🐞 or press `Shift+F9`
- App will pause at breakpoints

**Debug commands:**
- **Step Over:** `F8`
- **Step Into:** `F7`
- **Step Out:** `Shift+F8`
- **Resume:** `F9`

### Common Issues

**Issue:** Build fails with "Keystore not found"
**Fix:** Create `keystore.properties` or use debug build: `./gradlew assembleDebug`

**Issue:** App crashes on launch with "Activity class does not exist"
**Fix:** Check package name - debug builds use `.debug` suffix:
```bash
# Debug
adb shell monkey -p org.voidstream.androidtv.debug 1

# Release
adb shell monkey -p org.voidstream.androidtv 1
```

**Issue:** OTA update check crashes on Amazon/Google Play flavors
**Fix:** Ensure `UpdateCheckerService` DI registration is gated:
```kotlin
if (!BuildConfig.IS_AMAZON_BUILD && !BuildConfig.IS_GOOGLE_PLAY_BUILD) {
    single { UpdateCheckerService(get(), get()) }
}
```

**Issue:** Gradle sync fails with dependency resolution error
**Fix:**
```bash
./gradlew build --refresh-dependencies
# Or delete .gradle/ directory and re-sync
```

**Issue:** "SDK location not found"
**Fix:** Create `local.properties`:
```properties
sdk.dir=/path/to/android/sdk
```

**Issue:** Emulator won't start
**Fix:**
```bash
# Check available AVDs
emulator -list-avds

# Start with verbose logging
emulator -avd Android_TV_1080p_API_33 -verbose

# Or reinstall system image in SDK Manager
```

## IDE Setup

### Android Studio Configuration

**Recommended settings:**

1. **Kotlin auto-import:**
   - File → Settings → Editor → General → Auto Import
   - Check "Add unambiguous imports on the fly"
   - Check "Optimize imports on the fly"

2. **Code style:**
   - File → Settings → Editor → Code Style → Kotlin
   - Scheme: Project
   - Max line length: 120

3. **ktlint plugin:**
   - File → Settings → Plugins
   - Search "ktlint"
   - Install "ktlint" plugin
   - Restart Android Studio

4. **Live templates:**
   - File → Settings → Editor → Live Templates
   - Kotlin → Add frequently used snippets

### Run Configurations

**Create custom run configuration:**

1. Run → Edit Configurations
2. Click **+** → Android App
3. Configure:
   - **Name:** VoidStream (GitHub Debug)
   - **Module:** VoidStream-FireTV.app.main
   - **Build Variant:** githubDebug
   - **Launch:** Default Activity
4. Click **OK**

**Useful configurations:**
- VoidStream (GitHub Debug)
- VoidStream (Amazon Debug)
- VoidStream (Google Play Debug)
- VoidStream Tests (Unit)
- VoidStream Tests (Instrumented)

### Useful Shortcuts

**General:**
- **Reformat code:** `Ctrl+Alt+L` (Windows/Linux) / `Cmd+Opt+L` (Mac)
- **Optimize imports:** `Ctrl+Alt+O` (Windows/Linux) / `Ctrl+Opt+O` (Mac)
- **Run app:** `Shift+F10`
- **Debug app:** `Shift+F9`
- **Search everywhere:** Double `Shift`

**Navigation:**
- **Go to class:** `Ctrl+N` (Windows/Linux) / `Cmd+O` (Mac)
- **Go to file:** `Ctrl+Shift+N` (Windows/Linux) / `Cmd+Shift+O` (Mac)
- **Go to symbol:** `Ctrl+Alt+Shift+N` (Windows/Linux) / `Cmd+Opt+O` (Mac)
- **Recent files:** `Ctrl+E` (Windows/Linux) / `Cmd+E` (Mac)

**Refactoring:**
- **Rename:** `Shift+F6`
- **Extract variable:** `Ctrl+Alt+V` (Windows/Linux) / `Cmd+Opt+V` (Mac)
- **Extract method:** `Ctrl+Alt+M` (Windows/Linux) / `Cmd+Opt+M` (Mac)

## Contributing Workflow

### 1. Create Feature Branch

```bash
# Update your fork
git fetch upstream
git checkout master
git merge upstream/master

# Create feature branch
git checkout -b feature/my-awesome-feature
```

### 2. Make Changes

- Follow code style guidelines
- Write tests for new features
- Update documentation if needed

### 3. Run Local Checks

```bash
# Run all checks (tests + lint)
./gradlew check

# Auto-fix code style
./gradlew ktlintFormat
```

### 4. Commit Changes

```bash
git add .
git commit -m "feat: add support for custom subtitles"
```

### 5. Push to Your Fork

```bash
git push origin feature/my-awesome-feature
```

### 6. Open Pull Request

1. Go to https://github.com/ToastyToast25/VoidStream-FireTV
2. Click "Pull requests" → "New pull request"
3. Click "compare across forks"
4. Select your fork and branch
5. Fill out PR template
6. Click "Create pull request"

### 7. Code Review

- Address feedback from maintainers
- Push additional commits if needed
- Be respectful and constructive

## Release Process

**For maintainers only** (requires write access to repository)

See [CLAUDE.md](../CLAUDE.md) section "Publishing an OTA Update" for detailed release steps.

**Quick reference:**

1. Bump version in `gradle.properties`
2. Update `CHANGELOG.md`
3. Build release: `./gradlew assembleGithubRelease`
4. Commit: `git commit -m "Release vX.Y.Z"`
5. Create GitHub release with `gh` CLI
6. Upload APK to release
7. Submit to Google Play/Amazon (if applicable)

## Resources

### Project Documentation

- [CONTRIBUTING.md](../CONTRIBUTING.md) - Contribution guidelines
- [ARCHITECTURE.md](ARCHITECTURE.md) - System architecture
- [CLAUDE.md](../CLAUDE.md) - Project instructions and CI/CD
- [CHANGELOG.md](../CHANGELOG.md) - Version history

### External Resources

- **Jellyfin docs:** https://jellyfin.org/docs/
- **Android TV guide:** https://developer.android.com/tv
- **ExoPlayer docs:** https://developer.android.com/guide/topics/media/exoplayer
- **Jetpack Compose:** https://developer.android.com/jetpack/compose
- **Kotlin docs:** https://kotlinlang.org/docs/home.html
- **Koin DI:** https://insert-koin.io/

### Community

- **GitHub Discussions:** https://github.com/ToastyToast25/VoidStream-FireTV/discussions
- **Issue Tracker:** https://github.com/ToastyToast25/VoidStream-FireTV/issues
- **Security:** See [SECURITY.md](../SECURITY.md)

## Getting Help

**Need help?**

1. Check [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines
2. Search existing GitHub issues and discussions
3. Ask in GitHub Discussions
4. Email: haydenrmccray@gmail.com (for private inquiries)

**Found a bug?**
- Report via [Bug Report template](https://github.com/ToastyToast25/VoidStream-FireTV/issues/new?template=bug_report.yml)

**Have a feature idea?**
- Suggest via [Feature Request template](https://github.com/ToastyToast25/VoidStream-FireTV/issues/new?template=feature_request.yml)

---

Happy coding! 🚀
