# Architecture

## Architecture overview
### Share role
- Foreground service owns a multi-client SOCKS5 listener on the hotspot-facing interfaces.
- Each accepted client has its own session; there is no single-client global slot.
- Hotspot lifecycle stays under Android/Samsung controls. The app detects connectivity and guides the user when required.

### Receive role
- User joins the sharing phone's Wi-Fi network using Android's ordinary Wi-Fi UI.
- A network callback observes the active Wi-Fi network and link properties.
- The gateway is derived from the active Wi-Fi route table; no fixed subnet is assumed.
- Receiver probes the share endpoint and performs an upstream reachability check.
- Phase 2 will add an TUN-to-SOCKS transport and Android VpnService packet forwarding. Phase 1 must never represent a link check as a full internet tunnel.

### Multiple receivers
Android/Samsung hotspot DHCP controls client addresses. A normal third-party app cannot reliably randomize DHCP leases. The server instead supports concurrent independent SOCKS5 sessions, supporting multiple receiver sessions.

### Lifecycle and tiles
Share and Receive are independent foreground services. Stop changes only that role and must never finish the Activity. Two Quick Settings tiles invoke the same actions. Android's tile UI affordances are used for long-press navigation; arbitrary long-press callbacks are not assumed.

## Network model
- Share listener: SOCKS5 no-auth prototype on the private hotspot LAN.
- Receiver: gateway discovered from active Wi-Fi routes, configured SOCKS5 port.
- Health: SOCKS5 handshake plus external reachability check.
- Future full tunnel: Android VpnService TUN -> independent TUN-to-SOCKS transport -> Share SOCKS5 endpoint.

No user traffic is routed through a third-party server in this design.
