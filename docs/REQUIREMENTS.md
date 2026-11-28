# Product requirements

The authoritative Wi-Fi Direct behavior is described in [WIFI_DIRECT_SPEC.md](WIFI_DIRECT_SPEC.md). These requirements define the current Phase 1 and Phase 2 test release.

## Product identity and scope

1. The app display name is exactly **Hotspot Pistolwhip'd**.
2. The project is a standalone Android application with project-owned source, application identity and release channel.
3. Share and Receive are independent roles, each controlled by one toggle.
4. Stopping either role must not close the app UI or stop the other role.
5. No root, cloud relay, remote VPN server, AI service or traffic inspection is required.

## Phase 1 — Share

6. Share creates a Wi-Fi Direct group. Android's conventional Hotspot/tethering API is not the transport.
7. The SSID is formed exactly as `DIRECT-WH` + `IP-[phonename]` = `DIRECT-WHIP-[phonename]`. The implementation must not append an additional `IP-` segment.
8. The phone-name portion is normalized to SSID-safe ASCII and the full SSID stays within 32 bytes.
9. A fresh per-session passphrase is generated, 18–20 characters long, with uppercase and lowercase letters, at least five digits and at least five characters from `!@#$%^&*`. Credentials are not logged or persisted.
10. Share is shown active only after Android confirms group creation and the group credentials are verified.
11. The SOCKS5 listener starts only after group creation and binds to the Wi-Fi Direct group-owner interface. It must not be exposed on unrelated interfaces.
12. The Share panel shows the actual SSID, passphrase and available P2P endpoint information.
13. Credential transfer is user-initiated and limited to copy, a standard Wi-Fi QR code, and Android's standard Sharesheet. Sharesheet text contains only SSID and password; no fixed destination allowlist or automatic send is permitted.
14. Share supports multiple independent SOCKS5 TCP CONNECT and UDP ASSOCIATE sessions.

## Phase 2 — Receive and tunnel

15. Receive detects and validates an already-connected `DIRECT-WHIP-` network, or offers QR scanning and manual credential entry.
16. The app hands user-selected SSID/password to Android's supported Wi-Fi association request. Android owns connection approval and association; the app does not enumerate or silently select saved networks.
17. Receive must not establish a VPN while the user is still entering credentials or before the expected Wi-Fi Direct network and Share endpoint are verified.
18. Android VpnService consent is explicit. The VPN interface is established before the native tunnel starts.
19. The receiver tunnel forwards TCP and UDP through the Share SOCKS5 endpoint and routes IPv4, IPv6 and DNS as supported by the selected network.
20. The VPN is bound to the selected Wi-Fi network and the app's own UID is excluded to prevent proxy recursion.
21. Health remains red until the Share endpoint/upstream probe and native tunnel checks pass. A service toggle, group, listener or VPN interface alone is not proof of end-to-end forwarding.
22. Network loss, endpoint failure, VPN revocation or native engine failure clears the healthy state and stops the dependent tunnel.

## UI and platform behavior

23. Preserve true-black AMOLED styling, system-scaled Roboto/system typography, Material 3 dynamic accents, compact responsive spacing and the existing independent Quick Settings tiles.
24. Quick Settings tile taps toggle only their corresponding service; they do not open or foreground the app. Long-press opens the app through the tile settings activity. Persistent service notification taps open the app. Status-indicator taps do nothing.
25. Android/Samsung owns Wi-Fi Direct, Wi-Fi enablement, system approval and background execution. Where the platform blocks an operation, report the limitation and provide a system handoff rather than claiming success.
26. Android 10 or newer is required for custom Wi-Fi Direct group names and the app-assisted credential association flow. Older versions may use the manual Android Wi-Fi settings path where supported.

## Explicit exclusions

- No conventional Android hotspot/tethering transport.
- No hard-coded sharing destination list.
- No automatic credential sharing or upload.
- No saved-network enumeration or silent network selection.
- No phone-number prerequisite.
- No kill switch/lockdown behavior in this phase.
- No AI, cloud relay, remote VPN server, traffic inspection or automatic log upload.

## Readiness status

- Historical v0.1.x and v0.2.0 releases do not implement the approved Wi-Fi Direct topology.
- Phase 1/2 source commit e69af99a65031cf7b2238346a37b3356da1d04e6 passed GitHub Actions unit tests and debug APK assembly; native ABI checksum validation also passed.
- v0.3.0 release publication follows merge to main and its dedicated release workflow.
- Physical-device acceptance remains separate and is recorded in [TEST_PLAN.md](TEST_PLAN.md).
