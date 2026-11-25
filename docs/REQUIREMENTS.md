# Product requirements

The current authoritative requirements are in [WIFI_DIRECT_SPEC.md](WIFI_DIRECT_SPEC.md). This file records the key acceptance requirements and the history of the earlier prototypes.

## Confirmed product requirements

1. The sharing phone creates a Wi-Fi Direct group; Android's conventional Hotspot setting is not used as the local link.
2. The actual Wi-Fi Direct SSID is `DIRECT-WHIP-[phonename]`, combining the required `DIRECT-WH` group prefix with `IP-[phonename]`.
3. Share creates a per-session passphrase with the specified length and character composition; credentials are treated as secrets.
4. The Share SOCKS5 listener starts only after Android confirms group creation.
5. The Share panel expands only after the group and listener are established.
6. Share shows SSID/password, relevant verbose network/radio details and live statistics in a compact diagnostic layout.
7. The Share action offers a QR code, copy, and Android Sharesheet. Sharesheet uses all compatible installed destinations exposed by Android; no fixed app allowlist is maintained. The share payload contains only SSID and passphrase.
8. Receive enables Wi-Fi using the available device permission/capability, waits ten seconds, then checks the active SSID.
9. Receive accepts a matching `DIRECT-WHIP-` network; otherwise it offers QR scanning or manual credential entry.
10. The app passes user-selected network configuration and intent to Android. Android performs association and credential saving.
11. The receiver VPN is established only after association with the expected group and successful endpoint validation.
12. The VPN/tunnel is not reported ready until the VPN interface, native tunnel and end-to-end traffic probe are verified.
13. The UI preserves true-black AMOLED styling, system-scaled typography, dynamic accents, compact layout, independent Share/Receive controls and existing Quick Settings behavior.
14. Recovery attempts and log sharing follow the privacy and consent rules in WIFI_DIRECT_SPEC.md.
15. No root, cloud relay, remote VPN server, traffic inspection, automatic log upload or phone-number prerequisite is introduced.

## Historical prototypes

- v0.1.x and v0.2.0 are historical releases built around the earlier conventional-hotspot assumption.
- v0.2.0 starts a SOCKS5 listener and contains receiver VPN integration, but it does **not** create the Wi-Fi Direct group required by the current specification.
- The prior release instructions and architecture are superseded by the current Wi-Fi Direct specification. Do not use v0.2.0 as evidence that the new product flow works.

## Readiness gates

The project will progress through phases only when each gate is demonstrated:

1. **Wi-Fi Direct Share:** group creation with exact SSID prefix, generated credentials, listener bound to P2P interface, and truthful failure state.
2. **Receive association:** expected SSID detection, QR/manual credential handoff, Android association and saved-network behavior.
3. **VPN/tunnel:** VPN starts only after valid group association; native tunnel starts only after VPN establishment.
4. **End-to-end forwarding:** two physical phones demonstrate working TCP, UDP and DNS traffic through the Share phone; IPv6 where supported.
5. **UX and resilience:** compact verbose stats, Sharesheet/QR, background behavior, retry/log consent, and Samsung/One UI validation.

Automated build success is not physical-device acceptance.
