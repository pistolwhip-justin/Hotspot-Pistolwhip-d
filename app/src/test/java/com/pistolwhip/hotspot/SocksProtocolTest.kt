package com.pistolwhip.hotspot

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.InetAddress

class SocksProtocolTest {
    @Test fun serverPortIsStable() { assertEquals(1080, ShareService.PORT) }

    @Test fun decodesIpv4UdpDatagram() {
        val frame = byteArrayOf(0, 0, 0, 1, 1, 2, 3, 4, 0x01, 0xbb.toByte(), 9, 8)
        val decoded = SocksUdpCodec.decode(frame)
        assertNotNull(decoded)
        assertEquals("1.2.3.4", decoded!!.host)
        assertEquals(443, decoded.port)
        assertArrayEquals(byteArrayOf(9, 8), decoded.payload)
    }

    @Test fun decodesDomainUdpDatagram() {
        val host = "example.com".toByteArray(Charsets.UTF_8)
        val frame = byteArrayOf(0, 0, 0, 3, host.size.toByte()) + host + byteArrayOf(0, 53) + byteArrayOf(1, 2, 3)
        val decoded = SocksUdpCodec.decode(frame)
        assertNotNull(decoded)
        assertEquals("example.com", decoded!!.host)
        assertEquals(53, decoded.port)
        assertArrayEquals(byteArrayOf(1, 2, 3), decoded.payload)
    }

    @Test fun encodesIpv6ResponseDatagram() {
        val address = InetAddress.getByName("2001:db8::1")
        val frame = SocksUdpCodec.encode(address, 5353, byteArrayOf(4, 5))
        assertEquals(4, frame[3].toInt())
        assertEquals(5353, ((frame[20].toInt() and 255) shl 8) or (frame[21].toInt() and 255))
        assertArrayEquals(byteArrayOf(4, 5), frame.copyOfRange(22, frame.size))
    }

    @Test fun rejectsFragmentedAndMalformedFrames() {
        assertNull(SocksUdpCodec.decode(byteArrayOf(0, 0, 1, 1, 1, 2, 3, 4, 0, 53)))
        assertNull(SocksUdpCodec.decode(byteArrayOf(0, 0, 0, 9)))
        assertNull(SocksUdpCodec.decode(byteArrayOf(0, 0, 0, 1, 1, 2)))
    }
}
