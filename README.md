# 📡 Hotspot Pistolwhip'd

![Android](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white) ![Status](https://img.shields.io/badge/status-Phase%202%20test%20build%20released-brightgreen)

An Android hotspot-sharing companion from Pistol Whip Productions. One phone shares its connection over its Wi-Fi hotspot; another phone joins that Wi-Fi network normally and routes device traffic through the sharing phone.

## 🎯 What it does

- One **Share** toggle and one **Receive** toggle.
- Share phone hosts a multi-client SOCKS5 service on the private hotspot LAN.
- Receive phone joins through normal Android Wi-Fi settings, discovers the gateway and requests Android's VPN permission.
- Android VpnService captures receiver traffic and a native TUN-to-SOCKS5 engine forwards TCP and UDP through the Share phone.
- IPv4, IPv6 and DNS routing are configured for the receiver tunnel.
- Share supports SOCKS5 TCP CONNECT and UDP ASSOCIATE sessions.
- Separate Share and Receive Quick Settings tiles use the app mark; tapping a tile toggles its service without opening the app.
- Long-pressing a tile or tapping the persistent service notification opens the app.
- The compact, scrollable AMOLED-black dashboard uses system-scaled text, Material 3 dynamic accents and One UI-friendly spacing.
- Multiple receiver sessions are supported; Android/Samsung hotspot DHCP remains responsible for client IP allocation.
- No root, Wi-Fi Direct, cloud relay, traffic inspection or AI service is required. AI remains deferred.

## 🧭 Current development status

**Phase 1.1 — interaction polish:** released as v0.1.1. The existing test APK was exercised and user feedback was received.

**Phase 2 — device-wide VPN tunnel:** v0.2.0 test APK is published. CI compilation, unit tests, native dependency checksums and APK assembly passed. The APK includes Android VpnService, native TCP/UDP packet transport, SOCKS5 UDP relay support, DNS routing and VPN lifecycle handling.

The remaining readiness gate is physical two-device testing proving that traffic from apps on the receiving phone reaches the internet through the sharing phone. A successful build alone does not satisfy that gate.

## 🏗️ Architecture and decisions

- [Architecture](docs/ARCHITECTURE.md)
- [Requirements](docs/REQUIREMENTS.md)
- [Development plan](docs/DEVELOPMENT_PLAN.md)
- [Decision record](docs/DECISIONS.md)
- [Test plan](docs/TEST_PLAN.md)
- [Third-party notices](THIRD_PARTY_NOTICES.md)

The receiver uses [HevSocks5Tunnel](https://github.com/heiher/hev-socks5-tunnel) 2.20.0 (MIT) for the native TUN-to-SOCKS5 data plane. The four Android ABI libraries are downloaded by the build script and SHA-256 verified before packaging; they are not stored as opaque binaries in this repository.

## 📦 Latest published test APK

<badge>v0.2.0 — Phase 2 VPN Tunnel</badge>

- [Download APK](https://github.com/pistolwhip-justin/Hotspot-Pistolwhip-d/releases/download/v0.2.0/Hotspot-Pistolwhipd-v0.2.0-debug.apk)
- [GitHub Release notes](https://github.com/pistolwhip-justin/Hotspot-Pistolwhip-d/releases/tag/v0.2.0)

This build contains the receiver-side Android VPN/TUN tunnel and TCP/UDP SOCKS5 transport. It is a debug-signed sideload test build. **Uninstall v0.1.1 before installing v0.2.0:** the CI debug certificate changed between builds, so Android will reject an in-place update.

The earlier [v0.1.1 local-link prototype](https://github.com/pistolwhip-justin/Hotspot-Pistolwhip-d/releases/tag/v0.1.1) remains available for reference.

## 🧪 Build locally

Requires JDK 17, Android SDK API 35 and Gradle 8.9. The native libraries are pinned to HevSocks5Tunnel 2.20.0 and verified by SHA-256.

~~~bash
bash scripts/fetch-native-libs.sh
gradle wrapper --gradle-version 8.9 --gradle-distribution-sha256-sum=d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab
./gradlew --no-daemon testDebugUnitTest assembleDebug
~~~

The native dependency script supports arm64-v8a, armeabi-v7a, x86 and x86_64. GitHub Actions runs the same fetch, checksum, test and APK assembly steps from committed source.

Debug APKs are sideload test builds, not production or Play Store releases.

## 📱 How to test the Phase 2 build

1. Install the same Phase 2 APK on two Android phones.
2. On the sharing phone, enable its Android Wi-Fi hotspot in system settings and turn on **Share**.
3. On the receiving phone, join that hotspot as an ordinary Wi-Fi network.
4. Turn on **Receive** and approve Android's VPN permission when prompted.
5. Confirm the **Receive VPN** notification reports that the tunnel is active.
6. Browse from multiple apps on the receiving phone. Verify IPv4, IPv6 where available, DNS, TCP sites and UDP-dependent traffic.
7. Repeat with multiple receiver devices and test Wi-Fi disconnect/reconnect and Share stop/restart.

The Receive service checks that the sharing endpoint can establish a SOCKS5 connection to an external TCP endpoint before starting the VPN. A green service notification also requires the native tunnel engine to report that it is running. This health check is useful evidence, but the actual device-wide traffic test remains a physical-device acceptance check.

## ⚠️ Known limitations and validation

- CI compilation, unit tests, native ABI checksum verification and APK assembly passed for v0.2.0.
- Physical two-device TCP/UDP/DNS routing has not yet been verified; this APK is now ready for that test.
- IPv6 behavior depends on the underlying hotspot and upstream internet connection supporting IPv6.
- Android and Samsung control hotspot lifecycle, DHCP addresses and background execution.
- The app does not silently enable Wi-Fi or change hotspot DHCP leases.
- A debug-signed APK is for sideload testing only.
- No AI features are included in the current scope.

## 🏢 Project

Developed by **Pistol Whip Productions**.

---

Last updated: 2026-11-25
