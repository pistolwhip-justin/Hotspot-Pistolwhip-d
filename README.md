# 📡 Hotspot Pistolwhip'd

![Android](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white) ![Status](https://img.shields.io/badge/status-Phase%201.1%20test-orange)

A Pistol Whip Productions Android hotspot-sharing companion from Pistol Whip Productions. One phone shares its internet connection over its Wi-Fi hotspot; another phone joins that Wi-Fi network normally and activates a receiving link.


## 🎯 Product goals
- One Share toggle and one Receive toggle.
- Sharing phone hosts a multi-client SOCKS5 endpoint on its hotspot LAN.
- Receiving phone joins through Android's normal Wi-Fi controls, discovers the gateway and verifies the endpoint.
- Separate Quick Settings tiles toggle their own services without opening or foregrounding the app.
- Long-press either tile to open the app; tapping the persistent service notification also opens the app.
- The separate status indicator is non-interactive.
- True-black AMOLED interface, Roboto/system typography, Material 3 dynamic accents and One UI-inspired touch targets.
- AI concepts are deferred to a possible future phase; they are not part of the current build or core networking dependency.

## 🧭 Current phase

**Phase 1 — local-link prototype.** Includes app shell, independent controls, foreground services, a multi-client SOCKS5 listener and receiver-side gateway/endpoint health checks. It is not yet a complete internet tunnel; VPN packet forwarding is Phase 2.

**Phase 1.1 — interaction polish (released in v0.1.1).** Quick Settings taps operate silently in the background, tile long-press opens the app, notification tap opens the app, and app switches refresh when the user returns.

## 🏗️ Architecture and requirements
- [Architecture](docs/ARCHITECTURE.md)
- [Requirements](docs/REQUIREMENTS.md)
- [Development plan](docs/DEVELOPMENT_PLAN.md)
- [Decision record](docs/DECISIONS.md)
- [Test plan](docs/TEST_PLAN.md)

## 📦 Latest test APK

**v0.1.1 — Phase 1.1 Interaction Polish**

- [Download APK](https://github.com/pistolwhip-justin/Hotspot-Pistolwhip-d/releases/download/v0.1.1/Hotspot-Pistolwhipd-v0.1.1-debug.apk)
- [GitHub Release notes](https://github.com/pistolwhip-justin/Hotspot-Pistolwhip-d/releases/tag/v0.1.1)

Debug-signed sideload build. This is a local-link test build, not a complete internet tunnel or production release.

## 🧪 Build
Requires JDK 17 and Android SDK API 35. The GitHub Actions workflow creates the Gradle wrapper and runs unit tests plus the Android debug build.

```bash
gradle --no-daemon testDebugUnitTest assembleDebug
```

Debug APKs are sideload test builds, not production/Play Store releases.

## 📱 Phase 1 test
1. Enable the sharing phone's Android hotspot in system settings.
2. Turn on **Share** in the app.
3. Connect the receiving phone to that hotspot as an ordinary Wi-Fi network.
4. Turn on **Receive**. The app discovers the gateway and probes the SOCKS5 endpoint.
5. Use the separate Quick Settings tiles to toggle either service without opening the app.

**Important:** Phase 1 verifies the local link only. It does not yet route all receiver traffic through the sharing phone. Android/Samsung hotspot DHCP assigns client IP addresses; the app supports multiple independent clients rather than trying to set DHCP leases itself.

## ⚠️ Known limitations
- Full-device VPN packet forwarding is not implemented in Phase 1.
- Wi-Fi/hotspot controls remain subject to Android and Samsung permissions/system prompts.
- Physical-device validation is still required for One UI, gateway discovery, Quick Settings, notification interactions, background execution and multi-client behavior.
- No AI features are included in the current scope.

## 🏢 Project
Developed by **Pistol Whip Productions**. Final product naming, original artwork and distribution identity require Pistol Whip Productions approval.

---
Last updated: 2026-11-25
