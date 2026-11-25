# Architecture

## System overview

The product has two independent roles:

- **Share:** hosts a SOCKS5 proxy on the Wi-Fi hotspot's private LAN.
- **Receive:** joins that hotspot, establishes an Android VPN interface and forwards device traffic to the Share endpoint.

The receiver's normal Wi-Fi association is managed by Android. Wi-Fi Direct is not part of the connection flow.

## Share role

- A foreground Android service owns a multi-client SOCKS5 listener on TCP port 1080.
- TCP CONNECT requests create independent outbound TCP sockets.
- UDP ASSOCIATE requests create independent UDP relay sockets. The relay parses and emits RFC 1928 UDP datagrams, supports IPv4, IPv6 and domain-name destinations, and pins each association to the receiver address that opened its TCP control channel.
- Multiple TCP sessions and UDP associations can run concurrently.
- Hotspot lifecycle and DHCP address assignment remain under Android/Samsung controls. The app does not attempt to set or randomize DHCP leases.
- The endpoint is unauthenticated and is intended to be reachable only from the private hotspot network. Do not expose this listener to untrusted networks.

## Receive role and VPN lifecycle

1. The user joins the sharing phone's hotspot through Android Wi-Fi settings.
2. Receive observes Wi-Fi networks and obtains the active network's default gateway from LinkProperties.
3. The service opens a SOCKS5 session to the gateway and issues a TCP CONNECT probe to an external host. This verifies that the Share endpoint can reach an upstream destination.
4. Android's VPN consent is requested through VpnService.prepare() before starting the device-wide tunnel.
5. TunnelVpnService creates a TUN interface using Android's VpnService.Builder, configures IPv4 and IPv6 routes and DNS, and binds the VPN's underlying network to the selected Wi-Fi network.
6. The application package is excluded from its own VPN so its native SOCKS transport can reach the sharing phone without routing back into the TUN interface.
7. HevSocks5Tunnel converts TUN IP packets into SOCKS5 TCP CONNECT and UDP ASSOCIATE flows to the Share phone.
8. The native engine and tunnel file descriptor are stopped and closed when the user stops Receive, Android revokes VPN consent, or the underlying Wi-Fi network is lost.

## TUN-to-SOCKS engine

The receiver uses HevSocks5Tunnel 2.20.0, an MIT-licensed native engine:

- Upstream: https://github.com/heiher/hev-socks5-tunnel
- Pinned version: 2.20.0
- Native entry point: the bundled JNI contract in package hev.htproxy, class TProxyService.
- Data plane: Android TUN file descriptor -> native tun2socks -> SOCKS5 endpoint on the Share phone.
- UDP mode: standard SOCKS5 UDP ASSOCIATE.
- DNS: the VPN advertises a virtual IPv4 DNS address and the native engine's mapped-DNS feature preserves domain routing through the SOCKS endpoint.
- ABI coverage: arm64-v8a, armeabi-v7a, x86 and x86_64.

The ABI libraries are fetched from the pinned upstream release by scripts/fetch-native-libs.sh. The script verifies each SHA-256 before placing the library in the Android jniLibs source set. The binaries are not committed to the project repository. See THIRD_PARTY_NOTICES.md.

## Routing and health reporting

- The VPN includes default IPv4 and IPv6 routes. IPv6 behavior still depends on the hotspot and upstream network supporting IPv6.
- DNS is configured on the VPN interface; DNS packets are forwarded by the native tunnel rather than sent directly by the receiver's ordinary app sockets.
- The app's own package is excluded from the VPN to prevent proxy recursion. Its explicit SOCKS upstream probe uses the selected Wi-Fi Network socket factory.
- A green Receive notification requires a successful upstream SOCKS CONNECT probe and the native tunnel runtime to report that it is running.
- This health indicator is not a substitute for testing traffic from other application UIDs. End-to-end device-wide routing, DNS, IPv6 and UDP behavior must be verified on physical devices.

## Lifecycle and Quick Settings

Share and Receive are independent foreground services. Stopping one role must not close the Activity or stop the other role. Quick Settings tile taps toggle only their respective services; tile long-press and persistent notification taps open the app.

## Security and privacy boundaries

- No cloud relay or third-party VPN server is part of the architecture.
- No TLS interception or certificate installation is performed.
- No browsing history, credentials or traffic payloads are intentionally logged or uploaded.
- The local SOCKS5 endpoint uses no authentication in this test phase; keep it limited to the private hotspot LAN.
- The native dependency is MIT licensed and pinned with checksum verification.
