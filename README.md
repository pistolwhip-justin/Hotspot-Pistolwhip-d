# 📡 Hotspot Pistolwhip'd

![Android](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white) ![Status](https://img.shields.io/badge/status-Wi--Fi%20Direct%20implementation%20in%20development-orange)

A standalone Android app from Pistol Whip Productions for creating a direct phone-to-phone Wi-Fi link and carrying receiver traffic through an app-managed VPN tunnel. The sharing phone creates a **Wi-Fi Direct group**; Android's conventional Hotspot setting is not the transport.

## 🎯 Product purpose

- Share creates a Wi-Fi Direct group and local SOCKS5 endpoint.
- Receive joins the matching group and establishes Android's VPN before starting the TUN-to-SOCKS5 tunnel.
- The actual group SSID is `DIRECT-WHIP-[phonename]`.
- The Share panel displays compact live connection/radio diagnostics, the network credentials and a QR/share action.
- Credentials can be copied, shown as a QR code, or sent through Android's standard Sharesheet. Android chooses which installed compatible apps are offered; the app does not hard-code a destination list.
- The app never reports a listener, group, receiver, VPN or tunnel as active unless that specific state has been confirmed.
- True-black AMOLED UI, system-scaled typography, dynamic accents, independent Share/Receive controls and Quick Settings tiles remain part of the product.

## 🧭 Current development status

The v0.1.x and v0.2.0 releases are historical prototypes. **v0.2.0 does not create a Wi-Fi Direct group and is not the target architecture.** Its conventional-hotspot test instructions are superseded.

The authoritative Wi-Fi Direct specification is [docs/WIFI_DIRECT_SPEC.md](docs/WIFI_DIRECT_SPEC.md). Implementation is being reorganized into readiness-gated phases; CI success alone will not be treated as physical-device acceptance.

## 🏗️ Architecture and decisions

- [Wi-Fi Direct product specification](docs/WIFI_DIRECT_SPEC.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Requirements](docs/REQUIREMENTS.md)
- [Development plan](docs/DEVELOPMENT_PLAN.md)
- [Decision record](docs/DECISIONS.md)
- [Test plan](docs/TEST_PLAN.md)
- [Third-party notices](THIRD_PARTY_NOTICES.md)

The receiver-side TUN-to-SOCKS5 data plane currently uses HevSocks5Tunnel 2.20.0 (MIT), with pinned native library checksums. Its integration will be retained only where it supports the approved Wi-Fi Direct architecture.

## 📦 Historical test releases

- [v0.2.0 — earlier VPN prototype](https://github.com/pistolwhip-justin/Hotspot-Pistolwhip-d/releases/tag/v0.2.0) — does not create the required Wi-Fi Direct group.
- [v0.1.1 — earlier local-link prototype](https://github.com/pistolwhip-justin/Hotspot-Pistolwhip-d/releases/tag/v0.1.1).

Do not use either historical release as a test of the newly specified Wi-Fi Direct flow.

## 🧪 Build locally

Requires JDK 17, Android SDK API 35 and Gradle 8.9.

```bash
bash scripts/fetch-native-libs.sh
gradle wrapper --gradle-version 8.9 --gradle-distribution-sha256-sum=d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab
./gradlew --no-daemon testDebugUnitTest assembleDebug
```

Debug APKs are sideload test builds, not production releases.

## ⚠️ Platform and validation notes

- Wi-Fi Direct group creation, peer association, VPN establishment and verified traffic forwarding are separate readiness states.
- Android permission and background-execution behavior varies by OS version and device manufacturer.
- Android's public Wi-Fi APIs own network association and credential persistence; the app presents the requested UI and hands the user's selection to Android.
- Android Sharesheet destinations depend on installed compatible apps and may vary by device.
- Physical two-device testing is required before claiming the complete network path works.

## 🏢 Project

Developed by **Pistol Whip Productions**.

---

Last updated: 2026-11-25
