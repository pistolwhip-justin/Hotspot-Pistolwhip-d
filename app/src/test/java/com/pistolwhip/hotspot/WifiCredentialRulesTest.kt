package com.pistolwhip.hotspot

import org.junit.Assert.*
import org.junit.Test

class WifiCredentialRulesTest {
    @Test fun ssidUsesApprovedConcatenationAndFitsLimit() {
        assertEquals("DIRECT-WHIP-A15", WifiCredentialRules.ssidForDeviceName("A15"))
        val long = WifiCredentialRules.ssidForDeviceName("This is a very long phone name")
        assertTrue(long.startsWith("DIRECT-WHIP-"))
        assertTrue(long.toByteArray(Charsets.UTF_8).size <= 32)
        assertFalse(long.contains("DIRECT-WHIP-IP-"))
    }

    @Test fun generatedPassphraseMeetsCompositionPolicy() {
        repeat(100) {
            val password = WifiCredentialRules.generatePassphrase()
            assertEquals(20, password.length)
            assertTrue(password.any { it in 'A'..'Z' })
            assertTrue(password.any { it in 'a'..'z' })
            assertTrue(password.count { it in '0'..'9' } >= 5)
            assertTrue(password.count { it in "!@#$%^&*" } >= 5)
            assertTrue(WifiCredentialRules.isValidPassphrase(password))
        }
    }

    @Test fun parsesStandardWifiQrForThisAppNetwork() {
        val password = "AbCdE23456!@#$%fghij"
        val parsed = WifiCredentialRules.parseWifiQr("WIFI:T:WPA;S:DIRECT-WHIP-A15;P:$password;;")
        assertNotNull(parsed)
        assertEquals("DIRECT-WHIP-A15", parsed!!.ssid)
        assertEquals(password, parsed.passphrase)
    }

    @Test fun rejectsNonAppNetworkAndWeakPassword() {
        assertNull(WifiCredentialRules.parseWifiQr("WIFI:T:WPA;S:OtherNetwork;P:Abc12345!;;"))
        assertNull(WifiCredentialRules.parseWifiQr("WIFI:T:nopass;S:DIRECT-WHIP-A15;;"))
        assertNull(WifiCredentialRules.parseWifiQr("WIFI:T:WPA;S:DIRECT-WHIP-A15;P:weak;;"))
    }

    @Test fun qrEscapesSpecialWifiFieldCharacters() {
        assertEquals("a\\;b\\,c\\:d\\\"e", WifiCredentialRules.escapeWifiQr("a;b,c:d\"e"))
    }
}
