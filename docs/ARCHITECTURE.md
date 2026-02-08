# VoidStream Architecture

## Overview

VoidStream is a Kotlin-based Android TV client for Jellyfin media servers, built with modern Android development practices. It's a fork of the official Jellyfin Android TV client with enhancements including plugin support, OTA updates (for sideloaded builds), and a store-compliant distribution system.

**Key characteristics:**
- Native Android TV/Fire TV application
- GPL v2 licensed core with proprietary plugin support
- Multi-flavor build system (GitHub, Amazon, Google Play)
- Direct communication with Jellyfin servers (no cloud services)
- Zero telemetry or analytics

## Technology Stack

- **Language:** Kotlin 2.1.20
- **Build System:** Gradle 8.12.1 with Kotlin DSL
- **UI Framework:** Jetpack Compose + XML views (gradual migration to Compose)
- **Media Playback:** ExoPlayer (Media3 1.5.0)
- **Dependency Injection:** Koin
- **Backend SDK:** Jellyfin SDK 1.6.1
- **Networking:** OkHttp 4.12.0
- **Coroutines:** Kotlin Coroutines 1.7.3
- **Target Platforms:** Android TV, Fire TV, Google TV (Android 8.0+ / API 26+)
- **Min SDK:** 26 (Android 8.0 Oreo)
- **Target SDK:** 34 (Android 14)

## Module Structure

VoidStream follows a multi-module architecture for better separation of concerns and build performance.

### app/

**Main application module** containing:

- UI screens (Jetpack Compose + legacy XML views)
- Navigation logic (Compose Navigation)
- Dependency injection configuration (Koin modules)
- Startup activity and application class
- Build flavor configurations

**Key packages:**
- `org.jellyfin.androidtv.ui` - UI screens and fragments
- `org.jellyfin.androidtv.data` - Data repositories and models
- `org.jellyfin.androidtv.util` - Utility classes and helpers
- `org.jellyfin.androidtv.integration` - Third-party service integrations
- `org.jellyfin.androidtv.di` - Dependency injection modules

### playback/

**Media playback engine:**

- ExoPlayer integration and configuration
- Video/audio codec handling
- Subtitle rendering and synchronization
- Playback state management
- Audio/video track selection
- Direct play vs transcoding logic

### preference/

**Settings and user preferences:**

- SharedPreferences wrapper with type safety
- Settings UI screens
- User configuration persistence
- Per-server preferences
- Profile-specific settings

### design/

**Shared UI components and theming:**

- Custom Android TV-optimized views
- Theme definitions and colors
- Common UI utilities
- D-pad navigation helpers
- Leanback UI components

## Build Flavors

VoidStream uses Gradle **product flavors** to support three distribution channels via the `distribution` flavor dimension:

| Flavor | Purpose | OTA Updates | Install Permission | Format |
|--------|---------|-------------|-------------------|--------|
| `github` | Sideloaded (GitHub Releases) | ✅ Yes | ✅ Yes | APK |
| `amazon` | Amazon Appstore | ❌ No | ❌ No | APK |
| `googleplay` | Google Play Store | ❌ No | ❌ No | AAB |

### Feature Gating Flags

Two compile-time boolean constants control flavor-specific features:

- `BuildConfig.IS_AMAZON_BUILD` - True only for Amazon flavor
- `BuildConfig.IS_GOOGLE_PLAY_BUILD` - True only for Google Play flavor

**Sideload-only features:**
```kotlin
if (!IS_AMAZON_BUILD && !IS_GOOGLE_PLAY_BUILD) {
    // OTA updates, donate button, install permission
}
```

**Amazon-specific features:**
```kotlin
if (IS_AMAZON_BUILD) {
    // Amazon IAP integration (future)
}
```

**Google Play-specific features:**
```kotlin
if (IS_GOOGLE_PLAY_BUILD) {
    // Google Play Billing (future)
}
```

### Manifest Split

Flavor-specific `AndroidManifest.xml` files are merged at build time:

- `app/src/main/AndroidManifest.xml` - Shared base manifest
- `app/src/github/AndroidManifest.xml` - Adds `REQUEST_INSTALL_PACKAGES` permission + FileProvider
- `app/src/amazon/AndroidManifest.xml` - Empty (no OTA-related entries)
- `app/src/googleplay/AndroidManifest.xml` - Empty (no OTA-related entries)

The Android Gradle Plugin automatically merges these manifests during compilation.

## Key Architectural Patterns

### Dependency Injection (Koin)

**Module organization:**
- `AppModule.kt` - Application-wide dependencies
- `MediaModule.kt` - Media playback dependencies
- `NetworkModule.kt` - API client and network dependencies

**Flavor-gated service registration:**
```kotlin
// In AppModule.kt
if (!BuildConfig.IS_AMAZON_BUILD && !BuildConfig.IS_GOOGLE_PLAY_BUILD) {
    single { UpdateCheckerService(get(), get()) }
}
```

**CRITICAL:** Services gated in DI must use safe-call operators where accessed:
```kotlin
// ❌ BAD - crashes if service not registered
private val updateChecker: UpdateCheckerService by inject()
updateChecker.checkForUpdates()

// ✅ GOOD - safe for all flavors
private val updateChecker: UpdateCheckerService? by lazy {
    if (!IS_AMAZON_BUILD && !IS_GOOGLE_PLAY_BUILD) get() else null
}
updateChecker?.checkForUpdates()
```

### OTA Update System (GitHub Flavor Only)

**Architecture:**

1. **UpdateCheckerService** - GitHub Releases API client
2. **StartupActivity** - Blocks app on forced updates
3. **UpdateCheckWorker** - Daily background check via WorkManager
4. **ForcedUpdateFragment** - Full-screen update prompt
5. **WhatsNewFragment** - Post-update changelog display

**Update flow:**

```
App Launch
    ↓
StartupActivity checks for forced update
    ↓
[FORCE] tag in latest release? ───Yes→ Block app, show ForcedUpdateFragment
    ↓ No
Continue to main app
    ↓
UpdateCheckWorker (runs daily in background)
    ↓
New version available?
    ↓
Download APK → Verify SHA-256 → Prompt install
    ↓
Save release notes to SharedPreferences
    ↓
User installs APK
    ↓
App relaunches → Show WhatsNewFragment → Clear notes
```

**Components:**

- `UpdateCheckerService.kt` - GitHub API integration, APK download, checksum verification
- `UpdateCheckWorker.kt` - WorkManager periodic job
- `ForcedUpdateFragment.kt` - Blocking update screen (prevents app usage)
- `WhatsNewFragment.kt` - Post-update changelog display
- `UpdateActivity.kt` - Manual update check from settings

**Publishing an OTA update:**

1. Bump version in `gradle.properties`
2. Build: `./gradlew assembleGithubRelease`
3. Create GitHub release with APK attached
4. Include `[FORCE]` in release body for forced updates
5. Include `[BETA]` tag for pre-release builds (only shown to beta users)

### Plugin Architecture (Coming Soon)

**Design goals:**
- Keep GPL v2 core separate from proprietary plugins
- Plugins as separate APKs (not compiled with core)
- IPC communication via AIDL (Android Interface Definition Language)
- Monetize premium features without GPL contamination

**Architecture:**

```
VoidStream Core (GPL v2)
    ↓
AIDL Interface (public contract)
    ↓
Plugin APK (Proprietary)
    ├── IPTV Plugin
    ├── Advanced Playback Plugin
    └── [Future plugins]
```

**First plugin: IPTV**
- M3U playlist support
- EPG (Electronic Program Guide)
- Xtream Codes API integration
- Stalker portal support

**Plugin discovery:**
- Plugins declare `<service>` with `org.voidstream.plugin` action
- Core app queries PackageManager for installed plugins
- User enables/disables plugins in settings

## Data Flow

### Server Connection Flow

```
1. User enters server URL in ServerSetupScreen
      ↓
2. SessionRepository.discoverServer()
   - DNS-SD discovery (local network)
   - Direct URL connection (remote)
      ↓
3. User authenticates (username/password or quick connect)
      ↓
4. ApiClient initialized with credentials
   - Base URL stored
   - Access token saved to secure storage
      ↓
5. Fetch user profile and preferences
      ↓
6. Navigate to HomeFragment
```

**Key classes:**
- `SessionRepository` - Server discovery and session management
- `ApiClient` (Jellyfin SDK) - API communication
- `AuthenticationRepository` - Login/logout logic
- `ServerSetupScreen` - Compose UI for server setup

### Media Playback Flow

```
1. User selects content in UI (movie/episode/song)
      ↓
2. PlaybackLauncher determines playback strategy
   - Direct play (if codecs supported)
   - Transcode (if incompatible format)
      ↓
3. Start playback intent → VideoActivity
      ↓
4. VideoActivity initializes ExoPlayer
   - Load media source
   - Configure audio/subtitle tracks
   - Apply playback position (resume)
      ↓
5. User watches content
      ↓
6. Periodic progress updates to server (every 10s)
      ↓
7. Playback completes → Mark as watched → Return to UI
```

**Key classes:**
- `PlaybackLauncher` - Determines playback strategy
- `VideoActivity` (legacy) / `PlaybackActivity` (new) - Playback screen
- `PlaybackController` - ExoPlayer wrapper
- `MediaManager` - Media source preparation
- `PlaybackProgressManager` - Server sync

### Navigation Architecture

**Old (legacy XML fragments):**
- Activity-based navigation
- Fragment transactions
- Intent-based screen transitions

**New (Jetpack Compose):**
- Single-activity architecture
- Compose Navigation
- Type-safe navigation arguments
- Deep linking support

**Migration strategy:** Gradual migration from XML fragments to Compose screens. Both systems coexist during transition period.

## CI/CD Pipelines

**GitHub Actions workflows** (`.github/workflows/`):

1. **app-build.yaml** - Build debug APKs on push/PR
   - Triggers: Push to master, release branches, all PRs
   - Builds: GitHub, Amazon, Google Play debug variants
   - Artifacts: APKs retained for 14 days

2. **update-badges.yaml** - Auto-update README version badges
   - Triggers: Changes to `libs.versions.toml` or Gradle wrapper
   - Parses version numbers and updates shields.io badge URLs
   - Commits: `Update README version badges [skip ci]`

3. **ktlint.yaml** - Kotlin code style checks
4. **test.yaml** - Run unit tests
5. **lint.yaml** - Android lint checks
6. **release.yaml** - Build signed release APKs/AABs
7. **dependabot.yaml** - Automated dependency updates
8. **codeql.yaml** - Security scanning

## Security Considerations

### Permissions

**Required permissions:**
- `INTERNET` - Server communication (always required)
- `ACCESS_NETWORK_STATE` - Network availability checks
- `WAKE_LOCK` - Prevent screen sleep during playback

**Optional permissions:**
- `RECORD_AUDIO` - Voice search (degrades gracefully if denied)
- `REQUEST_INSTALL_PACKAGES` - OTA APK installation (github flavor only)

### Data Storage

**Sensitive data:**
- Access tokens - Encrypted with Android KeyStore
- Server URLs - SharedPreferences (plaintext, not sensitive)
- User preferences - SharedPreferences (plaintext)

**Cache:**
- Image cache - Disk cache (Coil image library)
- Video buffer - ExoPlayer cache (temporary)
- API responses - In-memory cache (short TTL)

### Network Security

- **TLS required** for remote connections (enforced by default)
- **Certificate pinning** - Not implemented (Jellyfin uses Let's Encrypt, pins would break)
- **Local network** - HTTP allowed for LAN connections

### Signing and Keystore

**Release builds:**
- Signed with `release.keystore` (RSA 2048-bit, JKS format)
- Keystore details in `keystore.properties` (git-ignored)
- Store password: `Frostbite2531!`
- Key password: `Frostbite2531!hrm`

**CRITICAL:** Losing the keystore means you cannot update any APK signed with it. Back up securely.

### GPL v2 Compliance

- All source code publicly available: https://github.com/ToastyToast25/VoidStream-FireTV
- Plugins kept separate to avoid GPL contamination (IPC-based architecture)
- Store listings include source code URL (required by GPL)
- Contributors must agree to GPL v2 (stated in CONTRIBUTING.md)

## Testing Strategy

### Unit Tests

**Location:** `app/src/test/`
**Framework:** JUnit 4 + Mockito + Kotlin Coroutines Test
**Run:** `./gradlew test`

**Coverage areas:**
- ViewModels and business logic
- Repositories and data sources
- Utility functions
- Update checker logic

### UI Tests (Instrumented)

**Location:** `app/src/androidTest/`
**Framework:** Espresso + Compose UI Test
**Run:** `./gradlew connectedAndroidTest`

**Coverage areas:**
- Navigation flows
- User authentication
- Settings screens
- Playback controls

### Manual Testing

**Test matrix:**
- 3 flavors (github, amazon, googleplay)
- 3 device types (Android TV, Fire TV, Google TV)
- = 9 test configurations

**Recommended devices:**
- Android TV emulator (1920x1080, API 33+)
- Fire TV Stick 4K (physical device)
- Nvidia Shield TV Pro (physical device)

## Common Development Tasks

### Adding a New Screen

**Compose-based screen:**

```kotlin
// 1. Create Composable in ui/screens/
@Composable
fun MyNewScreen(
    viewModel: MyViewModel = viewModel()
) {
    // Screen content
}

// 2. Add ViewModel
class MyViewModel(
    private val repository: MyRepository
) : ViewModel() {
    // State and logic
}

// 3. Register in Koin
single { MyViewModel(get()) }

// 4. Add navigation route
fun NavGraphBuilder.myNewScreenGraph() {
    composable("myNewScreen") {
        MyNewScreen()
    }
}

// 5. Add tests
class MyViewModelTest { ... }
```

### Adding a Store-Gated Feature

**Steps:**

1. Wrap feature code with build config checks:
```kotlin
if (!BuildConfig.IS_AMAZON_BUILD && !BuildConfig.IS_GOOGLE_PLAY_BUILD) {
    // OTA update code, donate button, etc.
}
```

2. If adding a DI bean, gate registration:
```kotlin
if (!IS_AMAZON_BUILD && !IS_GOOGLE_PLAY_BUILD) {
    single { MyFeatureService(get()) }
}
```

3. Use safe-call operators where accessing gated services:
```kotlin
private val myService: MyFeatureService? by lazy {
    if (!IS_AMAZON_BUILD && !IS_GOOGLE_PLAY_BUILD) get() else null
}
myService?.doSomething()
```

4. Add flavor-specific manifest entries (if needed):
   - `app/src/github/AndroidManifest.xml` - Add permissions/components

5. Test on all 3 flavors:
```bash
./gradlew assembleGithubDebug
./gradlew assembleAmazonDebug
./gradlew assembleGoogleplayDebug
```

### Releasing a New Version

**Process:**

1. **Bump version** in `gradle.properties`:
```properties
voidstream.version=2.0.2
```

2. **Update CHANGELOG.md**:
   - Move `[Unreleased]` items to new version section
   - Add release date
   - Update version links at bottom

3. **Build release APK/AAB**:
```bash
./gradlew assembleGithubRelease  # For GitHub releases (OTA)
./gradlew bundleGoogleplayRelease  # For Google Play (AAB)
./gradlew assembleAmazonRelease  # For Amazon Appstore
```

4. **Commit and tag**:
```bash
git add -A
git commit -m "Release v2.0.2"
git tag v2.0.2
git push origin master --tags
```

5. **Create GitHub release** (for github flavor):
```bash
gh api repos/ToastyToast25/VoidStream-FireTV/releases -X POST \
  -f tag_name="v2.0.2" \
  -f name="VoidStream v2.0.2" \
  -f body="Release notes here" \
  -F draft=false \
  -F prerelease=false
```

6. **Upload APK to GitHub release**
7. **Submit AAB to Google Play Console** (for googleplay flavor)
8. **Submit APK to Amazon Appstore** (for amazon flavor)

## Performance Considerations

### Image Loading

- **Library:** Coil 2.x (Compose-native)
- **Strategy:** Memory cache → Disk cache → Network
- **Optimization:** Pre-fetch thumbnails when scrolling lists

### Network Efficiency

- **API batching:** Group related API calls
- **Pagination:** Load content in chunks (50 items per page)
- **Compression:** Accept gzip/deflate encoding
- **Cache headers:** Respect server cache directives

### Playback Optimization

- **Direct play preferred:** Avoid transcoding when possible
- **Buffer ahead:** 30s forward buffer for smooth playback
- **Adaptive bitrate:** ExoPlayer handles automatically
- **Subtitle rendering:** Hardware-accelerated when supported

## External Resources

- **Jellyfin API docs:** https://jellyfin.org/docs/general/server/
- **ExoPlayer guide:** https://developer.android.com/guide/topics/media/exoplayer
- **Jetpack Compose:** https://developer.android.com/jetpack/compose
- **Kotlin conventions:** https://kotlinlang.org/docs/coding-conventions.html
- **Koin documentation:** https://insert-koin.io/
- **Android TV guidelines:** https://developer.android.com/tv

## Troubleshooting

### Build Issues

**Issue:** "Keystore not found"
**Fix:** Create `keystore.properties` or build debug variant

**Issue:** "SDK location not found"
**Fix:** Create `local.properties` with `sdk.dir=/path/to/android/sdk`

**Issue:** "Gradle sync failed"
**Fix:** `./gradlew build --refresh-dependencies`

### Runtime Issues

**Issue:** App crashes on startup (Amazon/Google Play flavors)
**Fix:** Check that `UpdateCheckerService` is not accessed on store builds

**Issue:** "Activity not found"
**Fix:** Check package name (debug builds use `.debug` suffix)

**Issue:** Playback fails
**Fix:** Check server transcoding settings and device codec support

---

This architecture document is a living document. Update it when making significant architectural changes or refactorings.
