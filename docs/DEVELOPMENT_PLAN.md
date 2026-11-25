# Development plan

Development proceeds through readiness-gated phases. The v0.1.x and v0.2.0 releases are historical prototypes and do not satisfy the current Wi-Fi Direct product specification.

## Phase 1 — Wi-Fi Direct Share foundation

- Add runtime permission handling for Wi-Fi Direct on supported Android versions.
- Create a Wi-Fi Direct group using the required SSID `DIRECT-WHIP-[phonename]`.
- Generate and retain the session passphrase securely in memory/state only as needed.
- Start the SOCKS5 listener only after successful group creation and bind/verify it on the P2P interface.
- Implement explicit state reporting for service, group and listener.
- Expand the Share UI only after actual group creation.
- Add automated tests for SSID normalization, password policy and state transitions.
- Update README, requirements, architecture, decisions and test plan.

**Gate:** CI build/tests pass; on a physical device, Share creates a visible P2P group with the required SSID, Android conventional Hotspot remains off, and a second device can associate with the group.

## Phase 2 — Receive discovery and connection handoff

- Enable Wi-Fi through the device's available app permission/capability; use a system handoff when unavailable.
- Wait ten seconds before reading the active SSID.
- Detect the expected `DIRECT-WHIP-` prefix and validate the connected network.
- Add QR and manual credential entry.
- Present an in-app Wi-Fi configuration UI matching Android's visual conventions and pass the user's selection/action to Android's supported Wi-Fi association API.
- Observe Android-managed saved-network reconnection rather than duplicating saved-network management.
- Add connection state, callbacks and failure handling.

**Gate:** two physical devices can connect by QR and manual entry; an already saved group can reconnect through Android; no false connected state is shown.

## Phase 3 — VPN-gated tunnel integration

- Retain the existing native TUN-to-SOCKS5 engine where compatible.
- Establish Android VpnService only after expected Wi-Fi Direct association and endpoint verification.
- Start native tunnel only after VPN establishment.
- Track VPN interface, native engine and traffic probe separately.
- Ensure failure/disconnect clears healthy status and stops dependent components.

**Gate:** physical receiver traffic passes through the sharing phone; TCP, UDP and DNS verified; IPv6 tested where supported.

## Phase 4 — Share presentation and diagnostics

- Compact verbose-style wireless information modeled on the user's supplied Wi-Fi diagnostics screenshot.
- Refresh visible statistics every five seconds and use Android network callbacks.
- QR generation, copy action and Android Sharesheet with SSID/passphrase only.
- Keep Share collapsed when off and auto-expand after successful group/listener creation.

**Gate:** credentials share correctly through QR and compatible Sharesheet destinations; diagnostic values match Android-reported network state.

## Phase 5 — resilience and final UX

- Three best-effort service restart attempts ten seconds apart.
- After repeated failure, ask whether to share logs; redact credentials and identifiers.
- Cancel, Share & Discard, and Share & Save actions.
- Best-effort recovery within Android foreground/background restrictions.
- Final Samsung/One UI, battery, accessibility and physical-device regression checks.

**Gate:** all consent, privacy, recovery and regression checks pass; final release is published from the exact source commit and verified.

## Existing foundation

- Standalone Kotlin/Android project and AMOLED Compose UI.
- Independent Share/Receive controls and Quick Settings tiles.
- Multi-client SOCKS5 TCP/UDP server implementation.
- Receiver VpnService and HevSocks5Tunnel 2.20.0 integration from the earlier prototype; physical forwarding was not accepted.
- v0.1.x and v0.2.0 releases remain historical reference points only.

No phase is considered complete based only on code being committed or CI passing. Physical-device gates remain explicit.
