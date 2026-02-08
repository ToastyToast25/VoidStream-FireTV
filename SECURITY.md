# Security Policy

## Supported Versions

We release security updates for the following versions of VoidStream:

| Version | Supported          |
| ------- | ------------------ |
| 2.0.x   | :white_check_mark: |
| < 2.0   | :x:                |

**Note:** Only the latest stable release receives security updates. We strongly recommend keeping VoidStream up to date.

## Reporting a Vulnerability

We take security seriously. If you discover a security vulnerability in VoidStream, please report it responsibly.

### 🔒 Private Disclosure (Recommended)

**For sensitive security issues**, please report privately:

1. **DO NOT** open a public GitHub issue
2. Email security concerns to: **haydenrmccray@gmail.com**
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

### Response Timeline

- **Initial response:** Within 48 hours
- **Status update:** Within 7 days
- **Fix timeline:** Depends on severity
  - **Critical:** Within 7 days
  - **High:** Within 14 days
  - **Medium:** Within 30 days
  - **Low:** Next regular release

### 📢 Public Disclosure

**For non-sensitive security issues** (e.g., outdated dependencies, low-severity findings):

- Open a GitHub issue with the `security` label
- We'll address it in the next release cycle

## Security Best Practices

### For Users

1. **Keep VoidStream updated** - Enable automatic updates in Google Play Store or Amazon Appstore
2. **Use HTTPS** - Always connect to your Jellyfin server over HTTPS
3. **Secure your server** - Ensure your Jellyfin server is properly secured
4. **Network security** - Use VPN or secure networks when accessing remote servers

### For Developers

1. **Dependencies** - We regularly update dependencies to patch known vulnerabilities
2. **Code review** - All pull requests undergo security review
3. **No sensitive data** - VoidStream does not collect or store user credentials on external servers
4. **Direct connections** - All connections are made directly to your own Jellyfin server

## Known Security Considerations

### Open Source Nature

VoidStream is licensed under GPL v2 and is fully open source. All code is publicly available at:
https://github.com/ToastyToast25/VoidStream-FireTV

### No External Data Collection

- VoidStream **does not** send any data to external servers
- All media connections are **direct to your own Jellyfin server**
- No analytics or tracking services are used
- Your media and credentials stay on your own infrastructure

### Plugin Security

VoidStream supports separate plugin modules (upcoming feature). When using plugins:

- Plugins are **not part of the GPL-licensed core**
- Plugins may have their own security policies
- Review plugin permissions before installation
- Only install plugins from trusted sources

## Security Hall of Fame

We recognize security researchers who responsibly disclose vulnerabilities:

<!-- Future contributors will be listed here -->
*No security vulnerabilities have been reported yet.*

## Security-Related Dependencies

VoidStream relies on:

- **Jellyfin SDK** - For server communication (security handled by Jellyfin project)
- **ExoPlayer (Media3)** - For media playback (maintained by Google)
- **OkHttp** - For network requests (industry-standard library)

All dependencies are regularly updated to incorporate security patches.

## Questions?

If you have questions about security in VoidStream:

- **General questions:** Open a GitHub Discussion
- **Security concerns:** Email haydenrmccray@gmail.com
- **Jellyfin server security:** Visit https://jellyfin.org/docs/general/security

---

**Last updated:** February 8, 2026
