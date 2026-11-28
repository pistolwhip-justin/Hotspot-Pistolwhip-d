# 📡 Hotspot Pistolwhip'd

![Android](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white) ![Status](https://img.shields.io/badge/status-Phase%201%20%26%202%20test%20release-orange)

A standalone Android app from Pistol Whip Productions for sharing one phone's internet connection with nearby phones over a direct Wi-Fi Direct group—without using Android's conventional Hotspot/tethering transport.

## 🎯 Phase 1 & 2

- Share creates a Wi-Fi Direct group using the exact SSID format **DIRECT-WHIP-[phonename]**.
- A fresh session passphrase is generated and shown only after group creation and the SOCKS endpoint are ready.
- The Share SOCKS5 TCP/UDP listener binds to the group-owner P2P interface.
- Credentials can be copied, displayed as a standard Wi-Fi QR code, or sent through Android's Sharesheet. No destination app list is hard-coded; Android offers compatible installed apps and the user chooses.
- Receive can scan the Share QR code or accept manual SSID/password entry. Android owns network approval and association.
- After the expected group and Share endpoint are verified, Receive requests Android VPN consent and starts the native TUN-to-SOCKS5 tunnel.
- The receiver tunnel supports TCP, UDP, DNS and IPv4/IPv6 routing as the underlying network permits.
- One Share toggle and one Receive toggle remain independent. Quick Settings tile taps toggle only their role; long-press and persistent notification taps open the app.
- True-black AMOLED UI, system-scaled typography and Material 3 dynamic accents.

## 📦 Phase 1 & 2 test APK

**v0.3.0 — Wi-Fi Direct Share + Receive**

- [GitHub Release page](https://github.com/pistolwhip-justin/Hotspot-Pistolwhip-d/releases/tag/v0.3.0)
- [Direct APK asset](https://github.com/pistolwhip-justin/Hotspot-Pistolwhip-d/releases/download/v0.3.0/Hotspot-Pistolwhipd-v0.3.0-debug.apk)

This is a debug-signed sideload test APK, not a production or Play Store release. If Android refuses to update an older build because its debug signing certificate differs, uninstall the previous test build and install v0.3.0.

## 📱 Test on two Android phones

1. Install the same v0.3.0 APK on both phones.
2. On phone A, turn Share on. Confirm the displayed network name is exactly DIRECT-WHIP-[phonename] and the conventional Android Hotspot was not enabled by the app.
3. Use the displayed QR code or Android Sharesheet to send only the SSID/password to phone B.
4. On phone B, turn Receive on, scan the QR or enter the credentials, and approve Android's Wi-Fi connection prompt.
5. Approve Android's VPN permission when asked.
6. Verify TCP web traffic, DNS and UDP-dependent apps through phone A. Check IPv6 when the group/upstream network supports it.
7. Test disconnect/reconnect, stopping Share, stopping Receive, multiple receiver devices, screen-off/background behavior and Samsung/One UI restrictions.

A green health indicator confirms the app's endpoint and tunnel checks; it does not replace testing real traffic from ordinary apps. Physical two-device acceptance remains a user/device test.

## 🏗️ Architecture and project docs

- [Wi-Fi Direct product specification](docs/WIFI_DIRECT_SPEC.md)
- [Requirements](docs/REQUIREMENTS.md)
- [Development plan](docs/DEVELOPMENT_PLAN.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Decision record](docs/DECISIONS.md)
- [Test plan](docs/TEST_PLAN.md)
- [Third-party notices](THIRD_PARTY_NOTICES.md)

The receiver uses [HevSocks5Tunnel](https://github.com/heiher/hev-socks5-tunnel) 2.20.0 (MIT), with native ABI libraries fetched and SHA-256 verified during CI. QR generation/scanning uses ZXing and JourneyApps Android Embedded; see third-party notices.

## 🛠️ Build from source

Requires JDK 17, Android SDK API 35 and Gradle 8.9.

~~~bash
bash scripts/fetch-native-libs.sh
gradle wrapper --gradle-version 8.9 --gradle-distribution-sha256-sum=d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab
./gradlew --no-daemon testDebugUnitTest assembleDebug
~~~

GitHub Actions runs the dependency checksum checks, unit tests and APK assembly from committed source.

## ⚠️ Limitations

- Android/Samsung owns Wi-Fi Direct group formation, network association, Wi-Fi settings and background execution.
- Android 10+ is required for the custom group SSID and app-assisted association.
- Physical two-device TCP/UDP/DNS forwarding, IPv6 and long-duration Samsung behavior must be validated separately from CI.
- No kill switch is implemented; if the VPN service exits, Android may restore normal routing.
- Detailed verbose radio statistics and automatic recovery/log-sharing are deferred to a later phase.
- AI, cloud relay, remote VPN server and traffic inspection are not part of the approved scope.

## 🏢 Project

Developed by **Pistol Whip Productions**.

---

Last updated: 2026-11-28
