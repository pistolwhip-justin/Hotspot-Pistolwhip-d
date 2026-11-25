# Decision record

## D-001 — Standalone application structure
- Date: 2026-11-24
- Decision: Maintain a standalone Kotlin/Android application with project-owned source, identifiers and distribution identity.
- Rationale: Keep implementation, branding and release ownership clear and maintainable.
- Status: Accepted.

## D-002 — Normal Wi-Fi join replaces Wi-Fi Direct
- Date: 2026-11-24
- Decision: Receiver joins sharing phone's hotspot using Android Wi-Fi UI, then activates Receive.
- Rationale: User reports this is easier and more reliable.
- Status: Accepted.

## D-003 — Concurrent receivers, not app-controlled DHCP
- Date: 2026-11-24
- Decision: Multi-client server with independent sessions. Do not claim to randomize DHCP client IPs.
- Rationale: Android/Samsung hotspot DHCP controls client addressing.
- Status: Accepted.

## D-004 — Link health is not a tunnel claim
- Date: 2026-11-24
- Decision: Until VPN forwarding exists, report endpoint/link health only.
- Rationale: Prevent false green status and misleading "tunnel active" claims.
- Status: Accepted.

## D-005 — Quick Settings and notification navigation
- Date: 2026-11-24
- Decision: Tapping a Share or Receive Quick Settings tile toggles only that service and must not launch, foreground, or otherwise bring the app Activity forward. Long-pressing either tile opens the app through the Android tile settings activity. Tapping the persistent service notification opens the app. Tapping the separate status indicator has no action.
- Rationale: Keep service controls independent from app navigation and avoid accidental UI interruptions.
- Status: Accepted.

## D-006 — AI is deferred
- Date: 2026-11-24
- Decision: Do not implement AI, network copilot, remote model calls, or AI-dependent features during the mass-testing foundation. Record possible concepts only in the future-phase backlog.
- Rationale: Prioritize a small, dependable networking product and get a test build into users' hands.
- Status: Accepted.


## D-007 — Native TUN-to-SOCKS5 engine

- Date: 2026-11-25
- Decision: Use HevSocks5Tunnel 2.20.0 (MIT) for receiver-side TUN packet conversion. Pin the upstream release and SHA-256 verify each Android ABI library during local/CI dependency preparation.
- Rationale: A maintained native tun2socks engine provides established TCP, UDP, IPv4 and IPv6 packet handling. Implementing a complete userspace TCP/IP stack in the app would materially increase correctness and maintenance risk.
- Boundaries: The Android VpnService, lifecycle, SOCKS endpoint and application integration remain project-owned. Preserve the upstream MIT notice.
- Status: Accepted; CI build, native checksums and v0.2.0 APK release passed. Physical validation remains pending.

## D-008 — Standard SOCKS5 UDP ASSOCIATE

- Date: 2026-11-25
- Decision: Extend the Share endpoint with RFC 1928 UDP ASSOCIATE alongside TCP CONNECT.
- Rationale: The receiver tunnel must carry UDP, including DNS and UDP-dependent application traffic, without relying on a separate relay service.
- Boundaries: UDP associations are tied to their TCP control connection and pinned to the requesting receiver address. The no-auth endpoint remains limited to the private hotspot LAN.
- Status: Implemented; unit tests and CI build passed in v0.2.0. Physical UDP/TCP validation remains pending.

## D-009 — Verified Receive health

- Date: 2026-11-25
- Decision: Receive health remains red until an upstream SOCKS5 TCP CONNECT probe succeeds and the native tunnel reports running. Share health becomes green only after its listener binds.
- Rationale: A service toggle or reachable TCP port alone is not evidence that the device-wide tunnel is active.
- Status: Implemented; CI build passed. Runtime behavior still requires physical two-device validation.

## D-010 — No unapproved kill switch

- Date: 2026-11-25
- Decision: Do not add Android lockdown/kill-switch behavior in this phase. Document that normal routing may resume if the VPN service exits.
- Rationale: A kill switch changes network availability and failure semantics and requires a separate explicit requirements and device-testing decision.
- Status: Accepted; limitation documented.
