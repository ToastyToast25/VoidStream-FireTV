# VoidStream Multi-Platform Flavor Release Plan

## Current State

VoidStream currently has two build flavors in the `distribution` dimension:

| Flavor | Status | Store | OTA Updates | Donate Button | Install Permission |
|--------|--------|-------|-------------|---------------|--------------------|
| `github` | Production | Sideloaded via GitHub Releases | Yes | Yes | Yes |
| `amazon` | Ready for submission | Amazon Appstore | No | No | No |

This plan covers adding two more distribution targets: **Google Play Store** (for Nvidia Shield and other Android TV devices) and **Roku Channel Store** (entirely separate platform).

---

## Part 1: Google Play Store Flavor (`googleplay`)

### Target Devices

- Nvidia Shield TV (Pro and Tube)
- Chromecast with Google TV
- Sony Android TV / Google TV
- TCL / Hisense Google TV
- Any Android TV device with Google Play Store

Nvidia Shield runs standard Android TV — there are **no Shield-specific APIs or certification requirements** beyond Google Play's Android TV quality guidelines.

### Google Play Store Restrictions

The following features are **prohibited** on Google Play and must be gated in the `googleplay` flavor:

#### 1. Self-Update / OTA Updates (PROHIBITED)

> "An app distributed via Google Play may not modify, replace, or update itself using any method other than Google Play's update mechanism. An app may not download executable code (such as dex, JAR, .so files) from a source other than Google Play."

- **Action:** Gate all OTA update code behind `IS_AMAZON_BUILD` → rename to `IS_STORE_BUILD` or add `IS_GOOGLE_PLAY_BUILD`
- **Alternative:** Reuse the same gating flag since Amazon and Google Play have the same restriction
- **Recommendation:** Rename `IS_AMAZON_BUILD` to `IS_STORE_BUILD` and set it to `true` for both `amazon` and `googleplay` flavors. This way one flag gates all store-restricted features.

#### 2. REQUEST_INSTALL_PACKAGES Permission (PROHIBITED for self-update)

> "The REQUEST_INSTALL_PACKAGES permission may not be used to perform self updates, modifications, or the bundling of other APKs."

- **Action:** Already removed via manifest split — reuse the same approach for `googleplay`

#### 3. External Monetization

**US Market (post-Epic v. Google ruling, October 2025):**
- Google **no longer requires** Google Play Billing for apps in the US
- Alternative payment methods and external payment links are **allowed**
- The donate button with QR code is technically **allowed** in the US

**Global (outside US and EEA):**
- Google Play Billing is still **required** for digital goods/subscriptions
- External payment links/donate buttons for digital goods are **prohibited**

**EEA (Digital Markets Act):**
- Developers can offer alternatives to Google Play Billing with a service fee (12-27%)

**Recommendation for VoidStream:**
- **Phase 1:** Hide the donate button on `googleplay` flavor (same as Amazon) for simplicity
- **Phase 2:** If targeting US-only, re-enable donate button for Google Play
- **Phase 3:** Implement Google Play Billing for global subscription support

#### 4. App Bundle Format (REQUIRED)

> "Apps must be submitted as Android App Bundle (AAB), not standalone APK."

- **Action:** Add `bundleGoogleplayRelease` task or use `./gradlew bundleGoogleplayRelease`
- **Output:** `.aab` file instead of `.apk`

#### 5. Content Rating (REQUIRED)

- Must complete IARC content rating questionnaire in Google Play Console
- Streaming apps face additional follow-up questions about content type
- **Action:** Complete during Play Console setup (not a code change)

### Android TV Quality Requirements for Google Play

These are **mandatory** for the app to appear in the TV section of Google Play:

#### Manifest Requirements

| Requirement | Current Status | Action Needed |
|-------------|---------------|---------------|
| `CATEGORY_LEANBACK_LAUNCHER` intent filter | Already declared | None |
| `android.hardware.touchscreen` with `required="false"` | Already declared | None |
| `android.software.leanback` feature | Already declared | None |
| Min SDK ≤ 31 | Currently 21 | None |

#### Asset Requirements

| Asset | Required Size | Current Status | Action Needed |
|-------|--------------|----------------|---------------|
| Home screen banner | 320 x 180 px (xhdpi) | Need to verify | Create if missing |
| App icon | 512 x 512 px | Already have (`LOGOS/`) | None |
| Screenshots | 1920 x 1080 | Already captured (`SCREENSHOTS/`) | Verify quality |
| Feature graphic | 1024 x 500 px | None | Create for Play Store |

#### D-pad Navigation

- All functionality must be navigable with 5-way D-pad (up, down, left, right, select)
- Back button must eventually return to Android TV home screen
- **Current status:** Already implemented and tested
- **Action:** Run full D-pad audit before submission

#### Media Playback

- Must provide "Now Playing" card on home screen for background audio
- Must support media key events (play, pause, rewind, fast-forward)
- **Current status:** Already implemented via Jellyfin base
- **Action:** Verify media keys work on Shield

#### Upcoming Requirements (August 1, 2026)

- Must support **64-bit architectures** (arm64)
- Must support **16 KB memory page sizes**
- **Action:** Verify these before submission deadline

### Implementation: Separate Flags (Option B) — IMPLEMENTED

Amazon and Google Play use **separate flags** because they have different payment systems (Amazon IAP vs Google Play Billing) and policies that may diverge:

```kotlin
// app/build.gradle.kts
productFlavors {
    create("github") {
        dimension = "distribution"
        buildConfigField("boolean", "IS_AMAZON_BUILD", "false")
        buildConfigField("boolean", "IS_GOOGLE_PLAY_BUILD", "false")
    }
    create("amazon") {
        dimension = "distribution"
        buildConfigField("boolean", "IS_AMAZON_BUILD", "true")
        buildConfigField("boolean", "IS_GOOGLE_PLAY_BUILD", "false")
    }
    create("googleplay") {
        dimension = "distribution"
        buildConfigField("boolean", "IS_AMAZON_BUILD", "false")
        buildConfigField("boolean", "IS_GOOGLE_PLAY_BUILD", "true")
    }
}
```

OTA/self-update gating uses both flags: `if (!BuildConfig.IS_AMAZON_BUILD && !BuildConfig.IS_GOOGLE_PLAY_BUILD)`.
Store-specific features use individual flags: `if (BuildConfig.IS_AMAZON_BUILD)` or `if (BuildConfig.IS_GOOGLE_PLAY_BUILD)`.

#### Files Changed

| File | Change |
|------|--------|
| `app/build.gradle.kts` | Added `googleplay` flavor with `IS_GOOGLE_PLAY_BUILD`, added `IS_GOOGLE_PLAY_BUILD` to existing flavors |
| `app/src/googleplay/AndroidManifest.xml` | Created (empty, same as Amazon — no install permission) |
| `AppModule.kt` | Added `&& !BuildConfig.IS_GOOGLE_PLAY_BUILD` to UpdateCheckerService gate |
| `JellyfinApplication.kt` | Added `&& !BuildConfig.IS_GOOGLE_PLAY_BUILD` to update worker gate |
| `StartupActivity.kt` | Added `&& !BuildConfig.IS_GOOGLE_PLAY_BUILD` to OTA/What's New gate |
| `SettingsMainScreen.kt` | Added `&& !BuildConfig.IS_GOOGLE_PLAY_BUILD` to all 3 update/donate gates |

#### Build Commands

```bash
# Google Play release (AAB for Play Store upload)
./gradlew bundleGoogleplayRelease
# Output: app/build/outputs/bundle/googleplayRelease/voidstream-androidtv-v*.aab

# Google Play release (APK for testing)
./gradlew assembleGoogleplayRelease
# Output: app/build/outputs/apk/googleplay/release/voidstream-androidtv-v*.apk
```

### Google Play Submission Checklist

- [x] Add `googleplay` flavor to `build.gradle.kts` with separate `IS_GOOGLE_PLAY_BUILD` flag
- [x] Add `IS_GOOGLE_PLAY_BUILD` to existing `github` and `amazon` flavors
- [x] Create `app/src/googleplay/AndroidManifest.xml` (empty)
- [x] Gate all OTA/update/donate code with `&& !BuildConfig.IS_GOOGLE_PLAY_BUILD`
- [ ] Build AAB: `./gradlew bundleGoogleplayRelease`
- [ ] Verify no `REQUEST_INSTALL_PACKAGES` in merged manifest
- [ ] Verify no OTA update code executes
- [ ] Verify no donate button in Settings
- [ ] Create Google Play Console developer account ($25 one-time fee)
- [ ] Complete content rating questionnaire (IARC)
- [ ] Create home screen banner (320x180 px)
- [ ] Create feature graphic (1024x500 px)
- [ ] Upload screenshots (1920x1080)
- [ ] Write store listing description
- [ ] Set up privacy policy URL (can use `AMAZON_PRIVACY_DISCLOSURE.md` hosted on GitHub)
- [ ] Submit for review
- [ ] Test on physical Nvidia Shield TV

---

## Part 2: Roku Channel Store

### Critical Finding: Roku is NOT Android

**Roku cannot run Android APKs.** Roku uses a completely proprietary platform:

- **Language:** BrightScript (Roku's scripting language, similar to Basic/JavaScript)
- **UI Framework:** SceneGraph (RSG) — XML-based, similar to HTML
- **Runtime:** Roku OS (not Android, not Linux-based in any compatible way)
- **Distribution:** Roku Channel Store (proprietary submission process)

**A Roku version of VoidStream would require building an entirely new app from scratch.** No code, layouts, or resources can be shared with the Android version.

### Roku Development Overview

#### Technology Stack

| Component | Android (current) | Roku (required) |
|-----------|-------------------|-----------------|
| Language | Kotlin | BrightScript |
| UI | Jetpack Compose + XML | SceneGraph (RSG) XML |
| Media player | ExoPlayer / Media3 | Roku native video node |
| API client | Jellyfin Kotlin SDK | Custom HTTP client (BrightScript) |
| DI | Koin | None (manual) |
| Build system | Gradle | Roku CLI / makefile |
| Package format | APK / AAB | ZIP (channel package) |

#### What Would Need to Be Built

1. **Jellyfin API client** in BrightScript (no SDK exists)
   - Authentication (username/password, quick connect)
   - Library browsing
   - Playback URL resolution
   - User session management
   - WebSocket for real-time events

2. **UI screens** in SceneGraph
   - Login / server selection
   - Home screen with media rows
   - Library browsing (movies, TV shows, music)
   - Item detail pages
   - Search
   - Settings
   - Player controls overlay

3. **Media playback** using Roku's native video node
   - Direct play and transcoding support
   - Subtitle rendering
   - Audio track selection
   - Resume/progress tracking

4. **Live TV** (if applicable)
   - EPG display
   - Channel switching
   - DVR support

#### Effort Estimate

Building a Roku channel for Jellyfin is a **major engineering project** — roughly equivalent to building the Android app from scratch. Estimated effort:

- **Minimum viable product (login + browse + play):** 3-6 months for one developer
- **Feature parity with Android app:** 6-12 months
- **Ongoing maintenance:** Separate codebase, separate testing, separate releases

#### Existing Roku Jellyfin Client

There is an existing community Roku client for Jellyfin: [jellyfin/jellyfin-roku](https://github.com/jellyfin/jellyfin-roku). Options:

1. **Fork it** (like we did with the Android TV client) and rebrand to VoidStream
2. **Build from scratch** for full control
3. **Skip Roku** until there's clear demand

**Recommendation:** Fork the existing Jellyfin Roku client if/when Roku support is needed. This is the same approach used for the Android TV client and saves months of development.

### Roku Channel Store Requirements

If pursuing Roku:

#### Certification Requirements

- Must function reliably across all Roku device models
- Must respond correctly to remote control input
- Must display error messages instead of freezing/crashing
- Must provide intuitive navigation and readable text
- Review takes at least **5 business days**

#### Monetization

- SVOD apps requiring login must support **Automatic Account Link** if streaming 1M+ hours/month
- Subscription price increases require **15 days customer notice**
- Roku takes a **20% revenue share** on paid channels

#### Upcoming Requirements (October 1, 2026)

- Apps must declare support for **RSG 1.3** in manifest

### Roku Decision

| Option | Effort | Recommendation |
|--------|--------|----------------|
| Skip Roku for now | None | **Recommended** — focus on Android TV stores first |
| Fork jellyfin-roku and rebrand | 2-4 weeks for basic rebrand | Good if demand exists |
| Build from scratch | 3-12 months | Not recommended |

---

## Part 3: Consolidated Flavor Architecture

### Final Target Architecture

```
distribution (flavor dimension)
├── github        → Sideloaded via GitHub Releases (OTA updates, donate button)
├── amazon        → Amazon Appstore (no OTA, no donate, no install permission)
├── googleplay    → Google Play Store (no OTA, no donate, AAB format)
└── (roku)        → Separate project entirely (BrightScript/SceneGraph)
```

### Gating Strategy — Separate Flags (IMPLEMENTED)

Amazon and Google Play use **separate flags** (`IS_AMAZON_BUILD` and `IS_GOOGLE_PLAY_BUILD`) to allow store-specific gating for different payment systems and policies:

| Feature | `github` | `amazon` | `googleplay` |
|---------|----------|----------|--------------|
| `IS_AMAZON_BUILD` | `false` | `true` | `false` |
| `IS_GOOGLE_PLAY_BUILD` | `false` | `false` | `true` |
| OTA update system | Enabled | Disabled | Disabled |
| What's New dialog | Enabled | Disabled | Disabled |
| Forced update screen | Enabled | Disabled | Disabled |
| Background update worker | Enabled | Disabled | Disabled |
| UpdateCheckerService DI | Registered | Not registered | Not registered |
| Check for Updates (Settings) | Shown | Hidden | Hidden |
| Update Notifications (Settings) | Shown | Hidden | Hidden |
| Beta Updates (Settings) | Shown | Hidden | Hidden |
| Donate button (Settings) | Shown | Hidden | Hidden |
| REQUEST_INSTALL_PACKAGES | In manifest | Not in manifest | Not in manifest |
| FileProvider | In manifest | Not in manifest | Not in manifest |
| Package format | APK | APK | AAB (+ APK for testing) |

### Priority Order

1. **Amazon Appstore** — Code changes done, needs submission (blocked by GPL v2 review)
2. **Google Play Store** — Code changes done. Unlocks Shield, Chromecast, Sony TV, etc. Needs Play Console setup.
3. **Roku** — Entirely separate project. Defer until demand justifies the effort.

---

## Implementation Steps

### Phase 1: Google Play Flavor — DONE

1. ~~Add `googleplay` flavor to `build.gradle.kts` with separate `IS_GOOGLE_PLAY_BUILD` flag~~
2. ~~Add `IS_GOOGLE_PLAY_BUILD = false` to existing `github` and `amazon` flavors~~
3. ~~Create `app/src/googleplay/AndroidManifest.xml` (empty)~~
4. ~~Gate all OTA/update/donate code paths with `&& !BuildConfig.IS_GOOGLE_PLAY_BUILD`~~
5. Build and verify: `./gradlew assembleGoogleplayRelease`
6. Verify AAB builds: `./gradlew bundleGoogleplayRelease`
7. Test on emulator

### Phase 2: Google Play Console Setup

1. Create Google Play Console developer account ($25 one-time fee)
2. Create app listing
3. Upload AAB
4. Complete content rating questionnaire (IARC)
5. Create required graphics (banner 320x180, feature graphic 1024x500)
6. Write store description
7. Set up privacy policy URL
8. Submit for review

### Phase 3: Nvidia Shield Testing

1. Install APK on physical Shield TV
2. Full D-pad navigation audit
3. Media playback testing (direct play + transcoding)
4. Voice search testing
5. Media key testing (play/pause/ff/rw)

### Phase 4: Roku (Deferred)

1. Evaluate demand
2. Fork jellyfin-roku if proceeding
3. Rebrand and customize
4. Submit to Roku Channel Store

---

## Reference: Store Policy Comparison

| Policy | Amazon Appstore | Google Play Store | Roku Channel Store |
|--------|-----------------|-------------------|--------------------|
| Self-update/OTA | Prohibited | Prohibited | N/A (different platform) |
| External payments | Prohibited (use Amazon IAP) | Allowed in US; Google Play Billing required globally | Roku takes 20% rev share |
| Install permission | Not needed | Not allowed for self-update | N/A |
| App format | APK | AAB (required) | ZIP (channel package) |
| Content rating | Amazon's own system | IARC questionnaire | Roku certification |
| Review time | 2-5 days | 1-7 days | 5+ business days |
| Developer fee | Free | $25 one-time | Free |
| Revenue share | 30% (20% for small dev) | 15% first $1M, then 30% | 20% |

## Reference: Google Play Policy Sources

- [Device and Network Abuse Policy](https://support.google.com/googleplay/android-developer/answer/16559646)
- [REQUEST_INSTALL_PACKAGES Policy](https://support.google.com/googleplay/android-developer/answer/12085295)
- [US Billing Policy Update (Epic ruling)](https://support.google.com/googleplay/android-developer/answer/15582165)
- [Google Play Payments Policy](https://support.google.com/googleplay/android-developer/answer/10281818)
- [TV App Quality Guidelines](https://developer.android.com/docs/quality-guidelines/tv-app-quality)
- [Android TV Apps Checklist](https://developer.android.com/training/tv/publishing/checklist)
- [NVIDIA Shield Deployment Checklist](https://developer.nvidia.com/android-tv-deployment-checklist)
- [Roku Certification Criteria](https://developer.roku.com/docs/developer-program/certification/certification.md)
