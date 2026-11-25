package com.pistolwhip.hotspot

import android.app.Service
import android.content.Intent
import android.os.IBinder
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.EOFException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ShareService : Service() {
    private val running = AtomicBoolean(false)
    private val clients = Executors.newCachedThreadPool()
    private val activeClients = ConcurrentHashMap.newKeySet<Socket>()
    @Volatile private var server: ServerSocket? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        startForeground(11, Notifications.build(this, "Sharing service", "SOCKS5 TCP/UDP endpoint listening on port $PORT"))
        if (running.compareAndSet(false, true)) clients.execute { listen() }
        return START_STICKY
    }

    private fun listen() {
        try {
            ServerSocket(PORT, 50).use { socket ->
                server = socket
                State.setShare(this, true)
                State.setShareHealthy(this, true)
                while (running.get()) {
                    val client = socket.accept()
                    activeClients.add(client)
                    clients.execute {
                        try { handleClient(client) }
                        finally { activeClients.remove(client) }
                    }
                }
            }
        } catch (_: Exception) {
            // Closing the listener is expected during an explicit stop.
        } finally {
            server = null
            running.set(false)
            State.setShare(this, false)
            State.setShareHealthy(this, false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun readExact(input: java.io.InputStream, length: Int): ByteArray {
        val bytes = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val count = input.read(bytes, offset, length - offset)
            if (count < 0) throw EOFException()
            offset += count
        }
        return bytes
    }

    private fun writeReply(output: java.io.OutputStream, code: Int, address: InetAddress?, port: Int) {
        val host = address ?: InetAddress.getByAddress(byteArrayOf(0, 0, 0, 0))
        val addr = host.address
        val atyp = if (addr.size == 16) 4 else 1
        output.write(byteArrayOf(5, code.toByte(), 0, atyp.toByte()))
        output.write(addr)
        output.write(byteArrayOf((port ushr 8).toByte(), port.toByte()))
        output.flush()
    }

    private fun handleClient(client: Socket) {
        client.use { s ->
            s.soTimeout = 15_000
            val input = BufferedInputStream(s.getInputStream())
            val output = BufferedOutputStream(s.getOutputStream())
            try {
                val version = input.read()
                val methods = input.read()
                if (version != 5 || methods !in 1..32) return
                val offered = readExact(input, methods)
                if (0.toByte() !in offered) {
                    output.write(byteArrayOf(5, 0xff.toByte())); output.flush(); return
                }
                output.write(byteArrayOf(5, 0)); output.flush()

                val request = readExact(input, 4)
                if (request[0].toInt() != 5 || request[2].toInt() != 0) return
                when (request[1].toInt() and 0xff) {
                    1 -> handleConnect(s, input, output, request[3].toInt() and 0xff)
                    3 -> handleUdpAssociate(s, input, output)
                    else -> writeReply(output, 7, null, 0)
                }
            } catch (_: Exception) {
                // A client closing or sending a malformed frame ends only its session.
            }
        }
    }

    private fun readTarget(input: java.io.InputStream, atyp: Int): String? {
        return when (atyp) {
            1 -> InetAddress.getByAddress(readExact(input, 4)).hostAddress
            3 -> {
                val length = input.read()
                if (length <= 0) null else String(readExact(input, length), Charsets.UTF_8)
            }
            4 -> InetAddress.getByAddress(readExact(input, 16)).hostAddress
            else -> null
        }
    }

    private fun readPort(input: java.io.InputStream): Int {
        val bytes = readExact(input, 2)
        return ((bytes[0].toInt() and 255) shl 8) or (bytes[1].toInt() and 255)
    }

    private fun handleConnect(s: Socket, input: java.io.InputStream, output: java.io.OutputStream, atyp: Int) {
        s.soTimeout = 0
        val host = readTarget(input, atyp) ?: run { writeReply(output, 8, null, 0); return }
        val port = readPort(input)
        try {
            Socket().use { remote ->
                remote.connect(InetSocketAddress(host, port), 12_000)
                writeReply(output, 0, remote.localAddress, remote.localPort)
                val upstream = Thread { try { input.copyTo(remote.getOutputStream()) } catch (_: Exception) { } }
                upstream.isDaemon = true
                upstream.start()
                try { remote.getInputStream().copyTo(output); output.flush() } catch (_: Exception) { }
                try { upstream.join(1000) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
            }
        } catch (_: Exception) {
            writeReply(output, 5, null, 0)
        }
    }

    /**
     * RFC 1928 UDP ASSOCIATE relay. One datagram socket handles both directions;
     * client packets are accepted only from the TCP control peer's IP address.
     */
    private fun handleUdpAssociate(s: Socket, input: java.io.InputStream, output: java.io.OutputStream) {
        s.soTimeout = 0
        DatagramSocket(0).use { relay ->
            relay.soTimeout = 1000
            writeReply(output, 0, s.localAddress, relay.localPort)
            val controlOpen = AtomicBoolean(true)
            val controlReader = Thread {
                try { while (input.read() >= 0) { /* control channel remains open */ } }
                catch (_: Exception) { }
                finally { controlOpen.set(false) }
            }
            controlReader.isDaemon = true
            controlReader.start()

            var clientEndpoint: InetSocketAddress? = null
            val buffer = ByteArray(65_507)
            while (running.get() && controlOpen.get() && !relay.isClosed) {
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    relay.receive(packet)
                    val source = InetSocketAddress(packet.address, packet.port)
                    if (packet.address == s.inetAddress) {
                        val pinned = clientEndpoint
                        if (pinned == null) clientEndpoint = source
                        if (clientEndpoint != source) continue
                        val request = SocksUdpCodec.decode(packet.data.copyOfRange(packet.offset, packet.offset + packet.length)) ?: continue
                        try {
                            val destination = InetAddress.getByName(request.host)
                            val payload = request.payload
                            relay.send(DatagramPacket(payload, payload.size, destination, request.port))
                        } catch (_: Exception) { }
                    } else {
                        val endpoint = clientEndpoint ?: continue
                        val payload = packet.data.copyOfRange(packet.offset, packet.offset + packet.length)
                        val frame = SocksUdpCodec.encode(packet.address, packet.port, payload)
                        relay.send(DatagramPacket(frame, frame.size, endpoint))
                    }
                } catch (_: SocketTimeoutException) {
                    // Poll the TCP control-channel state without busy-waiting.
                } catch (_: Exception) {
                    break
                }
            }
            controlOpen.set(false)
            try { controlReader.join(250) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
        }
    }

    override fun onDestroy() {
        running.set(false)
        try { server?.close() } catch (_: Exception) { }
        activeClients.forEach { try { it.close() } catch (_: Exception) { } }
        activeClients.clear()
        State.setShare(this, false)
        State.setShareHealthy(this, false)
        clients.shutdownNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val PORT = 1080
        const val ACTION_STOP = "com.pistolwhip.hotspot.STOP_SHARE"
    }
}
