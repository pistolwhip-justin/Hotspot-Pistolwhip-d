package com.pistolwhip.hotspot

import java.net.InetAddress

/** Encoder/decoder for the SOCKS5 UDP request header defined by RFC 1928. */
internal object SocksUdpCodec {
    data class Datagram(val host: String, val port: Int, val payload: ByteArray)

    fun decode(frame: ByteArray): Datagram? {
        if (frame.size < 4 || frame[0].toInt() != 0 || frame[1].toInt() != 0 || frame[2].toInt() != 0) return null
        var offset = 4
        val host = when (frame[3].toInt() and 0xff) {
            1 -> {
                if (frame.size < offset + 4 + 2) return null
                val bytes = frame.copyOfRange(offset, offset + 4); offset += 4
                InetAddress.getByAddress(bytes).hostAddress
            }
            3 -> {
                if (frame.size <= offset) return null
                val length = frame[offset++].toInt() and 0xff
                if (length == 0 || frame.size < offset + length + 2) return null
                val value = String(frame, offset, length, Charsets.UTF_8); offset += length
                value
            }
            4 -> {
                if (frame.size < offset + 16 + 2) return null
                val bytes = frame.copyOfRange(offset, offset + 16); offset += 16
                InetAddress.getByAddress(bytes).hostAddress
            }
            else -> return null
        }
        if (frame.size < offset + 2) return null
        val port = ((frame[offset].toInt() and 0xff) shl 8) or (frame[offset + 1].toInt() and 0xff)
        offset += 2
        if (port == 0) return null
        return Datagram(host, port, frame.copyOfRange(offset, frame.size))
    }

    fun encode(address: InetAddress, port: Int, payload: ByteArray): ByteArray {
        require(port in 1..65535)
        val addressBytes = address.address
        val atyp = if (addressBytes.size == 16) 4 else 1
        val result = ByteArray(4 + addressBytes.size + 2 + payload.size)
        result[0] = 0
        result[1] = 0
        result[2] = 0
        result[3] = atyp.toByte()
        System.arraycopy(addressBytes, 0, result, 4, addressBytes.size)
        val portOffset = 4 + addressBytes.size
        result[portOffset] = (port ushr 8).toByte()
        result[portOffset + 1] = port.toByte()
        System.arraycopy(payload, 0, result, portOffset + 2, payload.size)
        return result
    }
}
