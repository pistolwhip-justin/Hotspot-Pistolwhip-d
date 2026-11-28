# Development plan

Development proceeds through readiness-gated phases. The v0.1.x and v0.2.0 releases are historical prototypes built around the earlier conventional-hotspot assumptions; they do not satisfy the current Wi-Fi Direct design.

## Phase 1 — Wi-Fi Direct Share and credential handoff

- Establish a Share-side Wi-Fi Direct group using the exact approved SSID construction: `DIRECT-WH` + `IP-[phonename]` = `DIRECT-WHIP-[phonename]`. Do not add a second `IP-` segment.
- Generate a new session passphrase with the documented 18–20 character policy, including uppercase/lowercase, at least five digits and at least five approved special characters.
- Confirm group creation and returned credentials before showing Share as active.
- Bind the multi-client SOCKS5 TCP/UDP listener to the group-owner P2P interface, not a wildcard address on unrelated interfaces.
- Display SSID/password and P2P endpoint details.
- Share credentials only by user-initiated copy, standard Wi-Fi QR, or Android Sharesheet. The Sharesheet payload contains only SSID and password; do not hard-code destination apps or automatically send credentials.
- Keep secrets in memory for the active Share session only and clear them when Share stops.
- Preserve the OLED-black, system-scaled Material 3 UI and independent service/Quick Settings controls.

**Phase 1 automated gate:** unit tests for SSID construction, passphrase policy and QR parsing; compilation and APK assembly from committed source.

**Phase 1 device gate:** a real phone creates a visible Wi-Fi Direct group with the exact SSID, conventional Android Hotspot remains off, the P2P interface receives an address, and a second device can associate.

## Phase 2 — Receive association and device-wide tunnel

- If already connected to a matching `DIRECT-WHIP-` group, observe and validate that connection.
- Otherwise provide QR scanning and manual SSID/password entry.
- Pass user-selected credentials to Android's supported Wi-Fi network request/association flow. Android owns the approval UI and actual association; do not enumerate or silently choose saved networks.
- Do not start Receive VPN before the expected Wi-Fi Direct network and SOCKS endpoint have been verified.
- Establish Android VpnService only after the user grants consent and the expected network is associated.
- Start the pinned HevSocks5Tunnel 2.20.0 native engine only after the VPN interface is established.
- Preserve TCP CONNECT, UDP ASSOCIATE, IPv4/IPv6 routes and DNS forwarding from the prior prototype, and bind the VPN to the selected Wi-Fi network.
- Keep health red until the endpoint probe and native tunnel checks succeed. A running service, group, listener or VPN interface alone is not proof of end-to-end forwarding.
- Update README, architecture, requirements, decisions, test plan, third-party notices and the versioned release workflow.

**Phase 2 automated gate:** runtime/build dependencies verified; Kotlin compilation and unit tests pass; native ABI checksums pass; debug APK assembles and is published from the exact source commit.

**Phase 2 device gate:** two real Android phones associate through QR and manual entry; Receive VPN starts only after association; TCP, UDP and DNS traffic traverse the Share phone; IPv6 is checked when supported; disconnect/stop/restart behavior is recorded. CI success is not physical acceptance.

## Phase 3 — diagnostics and resilience (not part of this release)

- Compact verbose-style Wi-Fi Direct/radio statistics with five-second refresh and network callbacks.
- Best-effort restart attempts after unexpected service failure.
- Consent-based diagnostic log sharing and saving with credential/identifier redaction.
- Samsung/One UI background, battery, accessibility and long-duration validation.

This phase remains separate; no AI, cloud relay, remote VPN server, traffic inspection or automatic log upload is approved.
