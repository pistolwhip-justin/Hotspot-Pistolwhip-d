package com.pistolwhip.hotspot

import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import android.provider.Settings
import java.security.SecureRandom

/** Creates and removes the Share-side Wi-Fi Direct group. */
class WifiDirectGroup(private val context: Context) {
    data class Credentials(val ssid: String, val passphrase: String)

    private val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel = manager.initialize(context, Looper.getMainLooper(), null)
    @Volatile private var credentials: Credentials? = null

    fun create(onReady: (Credentials) -> Unit, onError: (String) -> Unit) {
        if (Build.VERSION.SDK_INT < 29) {
            onError("Custom Wi-Fi Direct group names require Android 10 or newer.")
            return
        }

        val deviceName = runCatching {
            Settings.Global.getString(context.contentResolver, "device_name")
        }.getOrNull().orEmpty().ifBlank { Build.MODEL.orEmpty() }
        val safeName = deviceName.replace(Regex("[^A-Za-z0-9_-]"), "").ifBlank { "Android" }.take(18)
        val ssid = "DIRECT-WHIP-IP-$safeName"
        val passphrase = generatePassphrase()
        val config = try {
            WifiP2pConfig.Builder().setNetworkName(ssid).setPassphrase(passphrase).build()
        } catch (error: Exception) {
            onError(error.message ?: "Could not prepare Wi-Fi Direct group credentials.")
            return
        }

        try {
            manager.createGroup(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    manager.requestGroupInfo(channel) { group: WifiP2pGroup? ->
                        val actual = group?.networkName.orEmpty()
                        val actualPassword = group?.passphrase.orEmpty()
                        if (actual.startsWith("DIRECT-WHIP-") && actualPassword.isNotBlank()) {
                            val result = Credentials(actual, actualPassword)
                            credentials = result
                            onReady(result)
                        } else {
                            onError("Android created a group but did not return the expected group credentials.")
                        }
                    }
                }

                override fun onFailure(reason: Int) {
                    onError("Wi-Fi Direct group creation failed (reason $reason).")
                }
            })
        } catch (error: SecurityException) {
            onError("Wi-Fi Direct permission is missing: ${error.message ?: "permission denied"}")
        } catch (error: Exception) {
            onError(error.message ?: "Wi-Fi Direct group creation failed.")
        }
    }

    fun remove(onComplete: (() -> Unit)? = null) {
        try {
            manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() { credentials = null; onComplete?.invoke() }
                override fun onFailure(reason: Int) { credentials = null; onComplete?.invoke() }
            })
        } catch (_: Exception) {
            credentials = null
            onComplete?.invoke()
        }
    }

    fun currentCredentials(): Credentials? = credentials

    private fun generatePassphrase(): String {
        val upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        val lower = "abcdefghijkmnopqrstuvwxyz"
        val digits = "23456789"
        val special = "!@#$%^&*"
        val random = SecureRandom()
        val chars = mutableListOf<Char>()
        repeat(5) { chars += upper[random.nextInt(upper.length)] }
        repeat(5) { chars += lower[random.nextInt(lower.length)] }
        repeat(5) { chars += digits[random.nextInt(digits.length)] }
        repeat(5) { chars += special[random.nextInt(special.length)] }
        for (i in chars.lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = chars[i]
            chars[i] = chars[j]
            chars[j] = temp
        }
        return chars.joinToString("")
    }
}
