# Decision record

## D-001 — Standalone application structure
- Date: 2026-11-24
- Decision: Maintain a standalone Kotlin/Android application with project-owned source, identifiers and distribution identity.
- Status: Accepted.

## D-002 — Wi-Fi Direct is the required local transport
- Date: 2026-11-25
- Decision: Share creates a Wi-Fi Direct group without enabling Android's conventional Hotspot setting. Receive joins that group and uses the receiver VPN/tunnel.
- Rationale: This is the defining product architecture and must not be replaced by conventional hotspot/tethering.
- Status: Accepted; implementation pending.

## D-003 — Exact group/network naming
- Date: 2026-11-25
- Decision: The actual SSID is `DIRECT-WHIP-[phonename]`, formed from group prefix `DIRECT-WH` plus friendly name `IP-[phonename]`.
- Rationale: Preserve the required Wi-Fi Direct prefix and consistent recognizable network identity.
- Status: Accepted.

## D-004 — Android Sharesheet for all compatible destinations
- Date: 2026-11-25
- Decision: Use Android's standard Sharesheet/chooser without a hard-coded app allowlist. Offer QR and copy as additional methods. The shared text payload contains only SSID and passphrase.
- Rationale: Let Android expose the communication apps installed on each device and let the user choose the destination.
- Status: Accepted; implementation pending.

## D-005 — Android owns Wi-Fi association and credential persistence
- Date: 2026-11-25
- Decision: The app provides the requested in-app visual configuration layer and passes the user's selected network credentials and action to Android's supported Wi-Fi APIs/system UI. Android performs the actual association and saving.
- Rationale: Avoid duplicating restricted system Wi-Fi management and use Android's own persistence behavior.
- Status: Accepted; implementation pending.

## D-006 — VPN starts only after valid Wi-Fi Direct association
- Date: 2026-11-25
- Decision: Receive waits ten seconds after Wi-Fi enablement, reads the active SSID, and only proceeds toward VPN establishment when the connected network matches `DIRECT-WHIP-` and endpoint validation succeeds.
- Rationale: Avoid starting a VPN against an unrelated network or before the local transport exists.
- Status: Accepted; implementation pending.

## D-007 — Truthful layered health states
- Date: 2026-11-25
- Decision: Report service, P2P group, listener, receiver association, VPN, native tunnel and verified traffic as separate states. Never infer full tunnel health from a local listener or service toggle.
- Rationale: Prevent false-positive status and misleading connection claims.
- Status: Accepted.

## D-008 — Recovery and log-sharing consent
- Date: 2026-11-25
- Decision: On unexpected Share service failure, make three best-effort restart attempts ten seconds apart; then offer Cancel, Share & Discard, or Share & Save. Redact credentials and identifiers; never send logs automatically.
- Rationale: Improve recovery without compromising user privacy.
- Status: Accepted; implementation pending.

## D-009 — Native TUN-to-SOCKS5 engine
- Date: 2026-11-25
- Decision: Retain HevSocks5Tunnel 2.20.0 (MIT) as the candidate receiver-side TUN packet engine, subject to successful integration with the Wi-Fi Direct group architecture.
- Rationale: A maintained native engine provides established TCP/UDP and IP packet handling.
- Status: Candidate retained; end-to-end physical validation pending.

## Superseded decisions

- Earlier decision that the receiver joins a conventional Android hotspot and that Wi-Fi Direct is not required is superseded by D-002.
- Earlier release instructions that tell the user to enable conventional hotspot are historical and must not be reused.
- The v0.2.0 listener/VPN prototype is not accepted as proof of Wi-Fi Direct or end-to-end tunnel functionality.
