# VoidStream-FireTV

## GitHub Repository

- **Origin:** https://github.com/ToastyToast25/VoidStream-FireTV
- **Upstream:** https://github.com/jellyfin/jellyfin-androidtv (original Jellyfin repo)

## Git Remotes

```bash
# Verify remotes
git remote -v

# Expected output:
# origin    https://github.com/ToastyToast25/VoidStream-FireTV.git (fetch)
# origin    https://github.com/ToastyToast25/VoidStream-FireTV.git (push)
# upstream  https://github.com/jellyfin/jellyfin-androidtv.git (fetch)
# upstream  https://github.com/jellyfin/jellyfin-androidtv.git (push)
```

## Pushing Changes

```bash
# Stage specific files
git add <file1> <file2>

# Stage all changes
git add -A

# Commit
git commit -m "Your commit message"

# Push to your repo
git push origin master
```

## Pulling Upstream Updates

```bash
# Fetch latest from original Jellyfin repo
git fetch upstream

# Merge upstream changes into your branch
git merge upstream/master
```

## MCP Servers

The following MCP servers are configured and available for use:

### Context7 (Library Documentation)
- **Purpose:** Fetch up-to-date library/API documentation, code examples, and setup guides
- **Tools:** `resolve-library-id`, `query-docs`
- **Usage:** Always use Context7 when needing library/API documentation, code generation, setup or configuration steps — without the user having to explicitly ask

### GitHub (GitHub API)
- **Purpose:** Full GitHub integration — issues, PRs, branches, commits, code search, reviews, releases
- **Tools:** `create_pull_request`, `list_issues`, `search_code`, `create_branch`, `add_issue_comment`, and many more
- **Account:** ToastyToast25
- **Repo:** ToastyToast25/VoidStream-FireTV

### Playwright (Browser Automation)
- **Purpose:** Browser automation for testing, screenshots, form filling, navigation
- **Tools:** `browser_navigate`, `browser_click`, `browser_snapshot`, `browser_take_screenshot`, etc.

### MarkItDown (File Conversion)
- **Purpose:** Convert files (PDF, Word, Excel, images, etc.) to markdown
- **Tools:** `convert_to_markdown`

### Desktop Commander (System Operations)
- **Purpose:** Persistent terminal sessions, file operations, process management, desktop screenshots
- **Tools:** `start_process`, `read_process_output`, `list_processes`, `kill_process`, `get_file_info`, etc.
- **Key advantage:** Long-running processes that persist beyond normal timeout limits

## Signing / Keystore

Keystore is generated and ready for release signing.

- **Keystore file:** `release.keystore` (JKS, RSA 2048-bit, valid ~27 years)
- **Key alias:** `voidstream`
- **Store password:** `Frostbite2531!`
- **Key password:** `Frostbite2531!hrm`
- **Config file:** `keystore.properties` (loaded by `app/build.gradle.kts`)
- **Template:** `keystore.properties.template` (safe to commit, has placeholder values)
- Both `release.keystore` and `keystore.properties` are git-ignored (secrets)

```bash
# Build signed release APK
./gradlew assembleRelease

# Output location
# app/build/outputs/apk/release/voidstream-androidtv-v*.apk

# Change keystore password (recommended)
keytool -storepasswd -keystore release.keystore
keytool -keypasswd -keystore release.keystore -alias voidstream

# Verify keystore
keytool -list -v -keystore release.keystore -alias voidstream
```

**IMPORTANT:** Back up `release.keystore` securely. If lost, you cannot update any APK signed with it.

## Publishing an OTA Update

Complete steps to bump version, build, push, and create a GitHub release.

### 1. Bump Version

Edit `gradle.properties`:

```properties
voidstream.version=X.Y.Z
```

### 2. Build Release APK

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/voidstream-androidtv-vX.Y.Z-release.apk
```

### 3. Commit and Push

```bash
git add -A
git commit -m "Description of changes — vX.Y.Z"
git push origin master
```

### 4. Create GitHub Release

Use `gh api` (not `gh release create` — it has auth scope issues):

```bash
# Create the release
# Use -F (not -f) for boolean fields (draft, prerelease)
# Include [FORCE] in the body to make it a forced update (blocks app until installed)
# Omit [FORCE] for optional updates (users can skip)
gh api repos/ToastyToast25/VoidStream-FireTV/releases -X POST \
  -f tag_name="vX.Y.Z" \
  -f target_commitish="master" \
  -f name="VoidStream vX.Y.Z" \
  -f body="Release notes here. Include [FORCE] to force update." \
  -F draft=false \
  -F prerelease=false \
  --jq '.id,.upload_url,.html_url'

# Upload the APK (replace RELEASE_ID with the id from above)
gh api "https://uploads.github.com/repos/ToastyToast25/VoidStream-FireTV/releases/RELEASE_ID/assets?name=voidstream-androidtv-vX.Y.Z-release.apk" \
  -X POST \
  --input "app/build/outputs/apk/release/voidstream-androidtv-vX.Y.Z-release.apk" \
  -H "Content-Type: application/vnd.android.package-archive" \
  --jq '.name,.size,.browser_download_url'
```

### Forced vs Optional Updates

The OTA updater checks the release body for `[FORCE]`:

- **`[FORCE]` present:** App blocks on the update screen — user must install to continue
- **`[FORCE]` absent:** Update is optional — app logs the available update and continues normally

### Pre-release / Beta Builds

Set `prerelease=true` when creating the release:

```bash
-F prerelease=true
```

Only users with "Beta updates" enabled in Settings will see pre-release builds.

### SHA-256 Verification

GitHub automatically provides a `digest` field (`sha256:xxx`) on release assets. The app verifies the downloaded APK checksum against this digest before installing. No manual steps needed.

### What's New Dialog

After a successful update, the app saves the release notes to SharedPreferences before installing. On next launch, a "What's New" dialog is shown automatically with the version and notes, then cleared.

## Licensing

- **License:** Proprietary (see `LICENSE`)
- **Branding:** "VoidStream" is proprietary — protected names, logos, and visual assets
- **Upstream:** Contains components from Jellyfin (GPL v2) — those components remain under GPL v2
- **Distribution:** Closed-source; users do not receive access to the code
- **Business model:** Monthly subscription or one-time purchase for Fire Stick users

## Project Info

- **Platform:** Android TV / Fire TV (Kotlin, Gradle)
- **Base:** Forked Jellyfin Android TV client
- **Application ID:** `org.voidstream.androidtv`
- **Namespace:** `org.jellyfin.androidtv` (preserved for Jellyfin SDK compatibility)
- **Goal:** Integrate with a custom IPTV plugin to be created
- **Brand:** Rebranded from "Moonfin" to "VoidStream"
- **OTA Updates:** Configured via GitHub Releases API (`ToastyToast25/VoidStream-FireTV`)
- **Logos:** Source files in `LOGOS/`, backups of old branding in `BACKUP-LOGOS/`
