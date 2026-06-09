package com.helpmethen.chat.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

object NetworkMonitor {

    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun register(
        context: Context,
        onAvailable: () -> Unit
    ): ConnectivityManager.NetworkCallback {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                onAvailable()
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)
        return callback
    }

    fun unregister(
        context: Context,
        callback: ConnectivityManager.NetworkCallback?
    ) {
        if (callback == null) {
            return
        }

        runCatching {
            context.getSystemService(ConnectivityManager::class.java)
                .unregisterNetworkCallback(callback)
        }
    }
}
