# Architecture

## Product topology

The app uses a direct Wi-Fi Direct group:

- **Share:** creates the group-owner network and hosts a multi-client SOCKS5 endpoint on the P2P interface.
- **Receive:** associates with that network through Android's user-approved Wi-Fi connection flow, establishes Android VpnService, and forwards device traffic to Share.

Android's conventional Hotspot/tethering API is not used as the Share transport. The required SSID is exactly `DIRECT-WHIP-[phonename]`; see [WIFI_DIRECT_SPEC.md](WIFI_DIRECT_SPEC.md).

## Share role

1. `WifiDirectGroup` requests a Wi-Fi Direct group using Android's `WifiP2pManager` and `WifiP2pConfig.Builder`.
2. The group SSID and passphrase are retrieved from Android's group information and validated against the requested value and credential policy.
3. `ShareService` waits for an IPv4 address on the group-owner P2P interface.
4. The SOCKS5 server binds directly to that P2P address and port 1080; accepted clients are additionally checked against the interface subnet.
5. Share health turns green only after the listener binds and starts accepting connections.
6. Credentials are held in process memory for the active Share session and cleared when Share stops.
7. The Share panel offers copy, a standard Wi-Fi QR code and Android Sharesheet. The share text contains only the SSID and password.

No credentials, phone number, hardware identifier or diagnostic data are included in the transfer payload or written to logs.

## Receive association

1. The user may choose a matching already-connected `DIRECT-WHIP-` network, or scan the Share QR code / enter its SSID and password.
2. The app validates the expected SSID prefix and passphrase policy.
3. The app passes the user's selection to Android through `WifiNetworkSpecifier`. Android owns the system approval and association; the app does not enumerate or silently select saved networks.
4. A network callback provides the associated `Network`. The service checks that the network is Wi-Fi and matches the expected SSID.
5. The service obtains the group-owner gateway from Android's `LinkProperties` where available, then performs a SOCKS5 TCP CONNECT probe over the selected network.
6. If the endpoint is unavailable, Receive remains unhealthy and the VPN is not started.

## VPN and TUN-to-SOCKS data plane

1. The user grants Android VpnService consent.
2. After expected network association and endpoint validation, `TunnelVpnService` establishes a TUN interface and binds the VPN's underlying network to the selected Wi-Fi network.
3. The app's own UID is excluded from its VPN to prevent the native SOCKS transport from recursively entering the TUN.
4. HevSocks5Tunnel 2.20.0 (MIT) converts TUN packets to SOCKS5 TCP CONNECT and UDP ASSOCIATE flows to the Share endpoint.
5. The tunnel config routes IPv4/IPv6 default traffic and DNS through the native engine. IPv6 remains dependent on the network's support.
6. The tunnel stops when Receive is stopped, the underlying network is lost, VPN consent is revoked, or the native engine fails.

Native ABI libraries for arm64-v8a, armeabi-v7a, x86 and x86_64 are fetched from the pinned upstream release and SHA-256 verified by `scripts/fetch-native-libs.sh`. They are not committed as opaque binary assets. See [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md).

## Layered health reporting

The app distinguishes:

1. Share service started.
2. Wi-Fi Direct group confirmed.
3. SOCKS5 listener bound to the P2P interface.
4. Receive associated with the expected group.
5. Share endpoint/upstream probe passed.
6. VPN interface established.
7. Native tunnel engine running.

A green service indicator is not a guarantee that arbitrary app traffic has been verified on a physical device. TCP, UDP, DNS and IPv6 acceptance remain device tests.

## Platform and privacy boundaries

- Android/Samsung owns group formation, network association, Wi-Fi settings, DHCP/IP assignment and background policy.
- Android 10+ is required for the custom group SSID and app-assisted local Wi-Fi request.
- The Share SOCKS endpoint is unauthenticated and is intended only for the private Wi-Fi Direct group. It binds to the P2P interface and checks peer subnet.
- No cloud relay, remote VPN server, TLS interception, traffic inspection, browsing-history collection, automatic log upload or AI service is part of this phase.
- No kill switch is implemented; if the VPN exits, Android may restore normal routing.
