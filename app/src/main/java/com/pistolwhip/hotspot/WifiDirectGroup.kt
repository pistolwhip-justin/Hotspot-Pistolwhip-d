package com.pistolwhip.hotspot

import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings

/** Creates and removes the Share-side Wi-Fi Direct group. */
class WifiDirectGroup(private val context: Context) {
    data class Credentials(val ssid: String, val passphrase: String)

    private val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel = manager.initialize(context, Looper.getMainLooper(), null)
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var credentials: Credentials? = null

    fun create(onReady: (Credentials) -> Unit, onError: (String) -> Unit) {
        if (Build.VERSION.SDK_INT < 29) {
            onError("Custom Wi-Fi Direct group names require Android 10 or newer.")
            return
        }

        val deviceName = runCatching {
            Settings.Global.getString(context.contentResolver, "device_name")
        }.getOrNull().orEmpty().ifBlank { Build.MODEL.orEmpty() }
        val ssid = WifiCredentialRules.ssidForDeviceName(deviceName)
        val passphrase = WifiCredentialRules.generatePassphrase()
        val config = try {
            WifiP2pConfig.Builder().setNetworkName(ssid).setPassphrase(passphrase).build()
        } catch (error: Exception) {
            onError(error.message ?: "Could not prepare Wi-Fi Direct group credentials.")
            return
        }

        try {
            manager.createGroup(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    confirmGroupCredentials(ssid, passphrase, 0, onReady, onError)
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

    private fun confirmGroupCredentials(
        expectedSsid: String,
        expectedPassphrase: String,
        attempt: Int,
        onReady: (Credentials) -> Unit,
        onError: (String) -> Unit
    ) {
        manager.requestGroupInfo(channel) { group: WifiP2pGroup? ->
            val actualSsid = group?.networkName.orEmpty()
            val actualPassphrase = group?.passphrase.orEmpty()
            if (actualSsid == expectedSsid && actualPassphrase == expectedPassphrase) {
                val result = Credentials(actualSsid, actualPassphrase)
                credentials = result
                onReady(result)
            } else if (attempt < 10) {
                handler.postDelayed({
                    confirmGroupCredentials(expectedSsid, expectedPassphrase, attempt + 1, onReady, onError)
                }, 500)
            } else {
                onError("Android created a group but did not return the exact requested credentials.")
            }
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

}
