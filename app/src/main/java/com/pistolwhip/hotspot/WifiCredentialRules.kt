package com.pistolwhip.hotspot

import java.security.SecureRandom

data class WifiCredentials(val ssid: String, val passphrase: String)

object WifiCredentialRules {
    const val SSID_PREFIX = "DIRECT-WHIP-"
    private const val MAX_SSID_BYTES = 32
    private const val UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val LOWER = "abcdefghijkmnopqrstuvwxyz"
    private const val DIGITS = "23456789"
    private const val SPECIAL = "!@#$%^&*"

    fun ssidForDeviceName(deviceName: String): String {
        val safe = deviceName.replace(Regex("[^A-Za-z0-9_-]"), "").ifBlank { "Android" }
        val available = MAX_SSID_BYTES - SSID_PREFIX.toByteArray(Charsets.UTF_8).size
        return SSID_PREFIX + safe.take(available)
    }

    fun generatePassphrase(random: SecureRandom = SecureRandom()): String {
        val chars = mutableListOf<Char>()
        repeat(5) { chars += UPPER[random.nextInt(UPPER.length)] }
        repeat(5) { chars += LOWER[random.nextInt(LOWER.length)] }
        repeat(5) { chars += DIGITS[random.nextInt(DIGITS.length)] }
        repeat(5) { chars += SPECIAL[random.nextInt(SPECIAL.length)] }
        for (i in chars.lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val value = chars[i]
            chars[i] = chars[j]
            chars[j] = value
        }
        return chars.joinToString("")
    }

    fun isValidPassphrase(value: String): Boolean =
        value.length in 18..20 &&
            value.any { it in 'A'..'Z' } &&
            value.any { it in 'a'..'z' } &&
            value.count { it in '0'..'9' } >= 5 &&
            value.count { it in SPECIAL } >= 5

    fun parseWifiQr(raw: String): WifiCredentials? {
        val value = raw.trim()
        if (!value.startsWith("WIFI:") || !value.endsWith(";;")) return null
        val fields = linkedMapOf<String, String>()
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var escaped = false
        for (char in value.removePrefix("WIFI:").dropLast(2)) {
            if (escaped) {
                current.append(char)
                escaped = false
            } else if (char == '\\\\') {
                escaped = true
            } else if (char == ';') {
                parts += current.toString()
                current.setLength(0)
            } else {
                current.append(char)
            }
        }
        if (escaped) current.append('\\\\')
        if (current.isNotEmpty()) parts += current.toString()
        for (part in parts) {
            val separator = part.indexOf(':')
            if (separator > 0) fields[part.substring(0, separator)] = part.substring(separator + 1)
        }
        val type = fields["T"]?.uppercase() ?: return null
        if (type !in setOf("WPA", "WPA2", "WPA3")) return null
        val ssid = fields["S"]?.takeIf { it.startsWith(SSID_PREFIX) } ?: return null
        val password = fields["P"] ?: return null
        if (!isValidPassphrase(password)) return null
        return WifiCredentials(ssid, password)
    }

    fun escapeWifiQr(value: String): String = buildString {
        value.forEach { char ->
            if (char in charArrayOf('\\\\', ';', ',', ':', '"')) append('\\\\')
            append(char)
        }
    }
}
