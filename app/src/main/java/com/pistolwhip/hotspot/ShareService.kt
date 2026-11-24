package com.pistolwhip.hotspot

import android.app.Service
import android.content.Intent
import android.os.IBinder
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ShareService : Service() {
    private val running = AtomicBoolean(false)
    private val clients = Executors.newCachedThreadPool()
    @Volatile private var server: ServerSocket? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        startForeground(11, Notifications.build(this, "Sharing service", "SOCKS5 endpoint listening on port $PORT"))
        if (running.compareAndSet(false, true)) clients.execute { listen() }
        return START_STICKY
    }

    private fun listen() {
        try {
            ServerSocket(PORT, 50).use { socket ->
                server = socket
                while (running.get()) {
                    val client = socket.accept()
                    clients.execute { handleClient(client) }
                }
            }
        } catch (_: Exception) {
            // Socket closure is expected during explicit stop; unexpected failure clears active state.
        } finally {
            server = null
            running.set(false)
            State.setShare(this, false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun handleClient(client: Socket) {
        client.use { s ->
            s.soTimeout = 15_000
            val input = BufferedInputStream(s.getInputStream())
            val output = BufferedOutputStream(s.getOutputStream())
            try {
                val version = input.read()
                val methods = input.read()
                if (version != 5 || methods < 1) return
                val offered = ByteArray(methods)
                if (input.read(offered) != methods || 0 !in offered) return
                output.write(byteArrayOf(5, 0)); output.flush()

                val header = ByteArray(4)
                if (input.read(header) != 4 || header[0].toInt() != 5 || header[1].toInt() != 1) return
                val host = when (header[3].toInt() and 0xff) {
                    1 -> input.readNBytes(4).joinToString(".") { (it.toInt() and 255).toString() }
                    3 -> { val len = input.read(); String(input.readNBytes(len)) }
                    4 -> input.readNBytes(16).joinToString(":") { "%02x".format(it) }
                    else -> return
                }
                val portBytes = input.readNBytes(2)
                if (portBytes.size != 2) return
                val port = ((portBytes[0].toInt() and 255) shl 8) or (portBytes[1].toInt() and 255)
                try {
                    Socket(host, port).use { remote ->
                        output.write(byteArrayOf(5, 0, 0, 1, 0, 0, 0, 0, 0, 0)); output.flush()
                        val upstream = Thread { s.getInputStream().copyTo(remote.getOutputStream()) }
                        upstream.start()
                        remote.getInputStream().copyTo(s.getOutputStream())
                        upstream.join(1000)
                    }
                } catch (_: Exception) {
                    output.write(byteArrayOf(5, 5, 0, 1, 0, 0, 0, 0, 0, 0)); output.flush()
                }
            } catch (_: Exception) { }
        }
    }

    override fun onDestroy() {
        running.set(false)
        try { server?.close() } catch (_: Exception) { }
        State.setShare(this, false)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object {
        const val PORT = 1080
        const val ACTION_STOP = "com.pistolwhip.hotspot.STOP_SHARE"
    }
}
