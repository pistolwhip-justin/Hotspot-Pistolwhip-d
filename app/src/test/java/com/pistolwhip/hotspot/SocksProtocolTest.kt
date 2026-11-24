package com.pistolwhip.hotspot

import org.junit.Assert.assertEquals
import org.junit.Test

class SocksProtocolTest {
    @Test fun serverPortIsStable() { assertEquals(1080, ShareService.PORT) }
}
