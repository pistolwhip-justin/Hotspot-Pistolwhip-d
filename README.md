# 📡 Hotspot Pistolwhip'd

![Android](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white) ![Status](https://img.shields.io/badge/status-Phase%201%20prototype-orange)

A clean-room Android hotspot-sharing companion from Pistol Whip Productions. One phone shares its internet connection over its Wi-Fi hotspot; another phone joins that Wi-Fi network normally and activates a receiving link.

This is a new implementation. No source code, binaries, assets, package names or branded identifiers are imported from the previous application.

## 🎯 Product goals
- One Share toggle and one Receive toggle.
- Sharing phone hosts a multi-client SOCKS5 endpoint on its hotspot LAN.
- Receiving phone joins through Android's normal Wi-Fi controls, discovers the gateway and verifies the endpoint.
- Later: opt-in auto-receive, separate Quick Settings tiles, resilient lifecycle notifications and full-device VPN forwarding.
- True-black AMOLED interface, Roboto/system typography, Material 3 dynamic accents and One UI-inspired touch targets.
- AI feature is deferred until core networking is stable; private network traffic will not be sent to an AI service.

## 🧭 Current phase

**Phase 1 — clean-room foundation and local-link prototype.** Includes app shell, independent controls, foreground services, a multi-client SOCKS5 listener and receiver-side gateway/endpoint health checks. It is not yet a complete internet tunnel; VPN packet forwarding is Phase 2.

## 🏗️ Architecture and requirements
- [Architecture](docs/ARCHITECTURE.md)
- [Requirements](docs/REQUIREMENTS.md)
- [Development plan](docs/DEVELOPMENT_PLAN.md)
- [Decision record](docs/DECISIONS.md)

## 📦 Latest test APK

**v0.1.0 — Phase 1 prototype**

- [Download APK](https://github.com/pistolwhip-justin/Hotspot-Pistolwhip-d/releases/download/v0.1.0/Hotspot-Pistolwhipd-v0.1.0-debug.apk)
- [GitHub Release notes](https://github.com/pistolwhip-justin/Hotspot-Pistolwhip-d/releases/tag/v0.1.0)

Debug-signed sideload build. This is a local-link prototype, not a complete internet tunnel.

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
5. The UI remains open when either role is stopped.

**Important:** Phase 1 verifies the local link only. It does not yet route all receiver traffic through the sharing phone. Android/Samsung hotspot DHCP assigns client IP addresses; the app supports multiple independent clients rather than trying to set DHCP leases itself.

## ⚠️ Known limitations
- Full-device VPN packet forwarding is not implemented in Phase 1.
- Wi-Fi/hotspot controls remain subject to Android and Samsung permissions/system prompts.
- Physical-device validation is still required for One UI, gateway discovery, Quick Settings, background execution and multi-client behavior.

## 🏢 Project
Developed by **Pistol Whip Productions**. Final product naming, original artwork and distribution identity require Pistol Whip Productions approval.

---
Last updated: 2026-11-24
