# Decision record

## D-001 — Clean-room implementation
- Date: 2026-11-24
- Decision: Build from an empty repository with new Kotlin/Android implementation.
- Rationale: User explicitly requested no reuse of the prior application's code and independent distribution identity.
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
