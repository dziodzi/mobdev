package ru.ivan.mobchat.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

class NetworkMonitor(
    context: Context,
    private val onAvailable: () -> Unit,
    private val onLost: () -> Unit
) {
    private val connectivityManager =
        context.getSystemService(ConnectivityManager::class.java)
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            onAvailable()
        }

        override fun onLost(network: Network) {
            onLost()
        }
    }

    fun isOnline(): Boolean {
        return runCatching {
            val network = connectivityManager.activeNetwork ?: return@runCatching false
            val capabilities = connectivityManager.getNetworkCapabilities(network)
                ?: return@runCatching false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrDefault(false)
    }

    fun register() {
        runCatching { connectivityManager.registerDefaultNetworkCallback(callback) }
    }

    fun unregister() {
        runCatching { connectivityManager.unregisterNetworkCallback(callback) }
    }
}
