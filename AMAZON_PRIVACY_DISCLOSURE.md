# VoidStream Privacy & Data Collection Disclosure

**App Name:** VoidStream
**Developer:** ToastyToast25
**Last Updated:** February 2026

---

## Summary

VoidStream is a media streaming client that connects to the user's self-hosted Jellyfin media server. The app does not collect analytics, telemetry, or personal data for advertising purposes. All data transmission is to services explicitly configured by the user.

---

## Data Collected & Transmitted

### 1. Account & Authentication Data

| Data Type | Purpose | Destination | User-Initiated |
|-----------|---------|-------------|----------------|
| Username | Server authentication | User's Jellyfin server | Yes |
| Password | Server authentication | User's Jellyfin server | Yes |
| Access token | Session management | Stored locally only | Automatic |
| Server URL | Connect to media server | User's Jellyfin server | Yes |

**Storage:** Encrypted SharedPreferences on device. Access tokens are not included in Android backup.

### 2. Device Information

| Data Type | Purpose | Destination | User-Initiated |
|-----------|---------|-------------|----------------|
| Device model | Session identification | User's Jellyfin server | Automatic |
| Android version | Compatibility reporting | User's Jellyfin server | Automatic |
| App version | Session identification | User's Jellyfin server | Automatic |

**Note:** Device info is sent as standard HTTP headers to the user's own Jellyfin server for session identification. This is required by the Jellyfin protocol.

### 3. Playback & Viewing Data

| Data Type | Purpose | Destination | User-Initiated |
|-----------|---------|-------------|----------------|
| Watch progress | Resume playback | User's Jellyfin server | Automatic |
| Watched status | Track viewed content | User's Jellyfin server | Automatic |
| Search queries | Search media library | User's Jellyfin server | Yes |

**Note:** All playback data is sent exclusively to the user's own self-hosted server.

### 4. Crash & Issue Reports

| Data Type | Purpose | Destination | User-Initiated |
|-----------|---------|-------------|----------------|
| Stack traces | Bug fixing | GitHub Issues API | Yes (user-initiated only) |
| App logs | Debugging | GitHub Issues API | Yes (user-initiated only) |
| Device info | Debugging | GitHub Issues API | Yes (user-initiated only) |

**Sanitization:** All crash reports are automatically sanitized before transmission:
- Passwords, tokens, and credentials are redacted
- Email addresses are redacted
- IP addresses are redacted
- User IDs are redacted

**Note:** Issue reporting is entirely optional and user-initiated. No crash data is sent automatically.

### 5. Optional Third-Party Integrations (User-Configured)

These services are only contacted if the user explicitly configures them:

| Service | Data Sent | Purpose |
|---------|-----------|---------|
| Jellyseerr | Authentication, media request IDs | Media request management |
| TMDB (The Movie Database) | Movie/show IDs | Fetch ratings |
| MDBList | Movie/show IDs | Fetch aggregated ratings |

**Note:** None of these services are contacted unless the user provides API keys or server URLs in Settings.

---

## Data NOT Collected

- No advertising identifiers
- No analytics or telemetry
- No location data
- No contact information
- No browsing history outside the app
- No third-party tracking SDKs
- No data sold to third parties
- No cross-app tracking
- No data shared with data brokers

---

## Permissions Used

| Permission | Purpose | Required |
|------------|---------|----------|
| `INTERNET` | Connect to media server | Yes |
| `ACCESS_NETWORK_STATE` | Check connectivity before requests | Yes |
| `ACCESS_WIFI_STATE` | Detect WiFi for quality settings | Yes |
| `RECORD_AUDIO` | Voice search (on-device processing) | No (optional) |
| `FOREGROUND_SERVICE` | Background audio playback | Yes |
| `WAKE_LOCK` | Prevent sleep during playback | Yes |
| `RECEIVE_BOOT_COMPLETED` | Resume background services | Yes |
| `WRITE_EPG_DATA` | Android TV channel integration | Yes |

**RECORD_AUDIO:** Used exclusively for voice search via the device's built-in speech recognizer. Audio is processed on-device and is not transmitted to VoidStream or any third party. The app functions fully without this permission.

---

## Data Storage & Security

- **Local storage:** User preferences and authentication data are stored in Android SharedPreferences on the device
- **Network security:** The app supports HTTPS connections; cleartext HTTP is permitted for local network Jellyfin servers that do not use TLS
- **Backup:** User preferences are included in Android system backup. Access tokens are excluded
- **Data retention:** Data persists until the user removes their account or uninstalls the app

---

## Children's Privacy

VoidStream does not knowingly collect personal information from children under 13. The app connects to the user's own media server, which may have parental controls configured independently.

---

## Contact

For privacy questions: https://github.com/ToastyToast25/VoidStream-FireTV/issues
