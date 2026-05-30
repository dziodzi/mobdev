package ru.tsvetkova.chat.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class ConnectivityObserver(context: Context) {

    private val manager = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun currentlyOnline(): Boolean {
        val active = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(active) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    val isOnline: Flow<Boolean> = callbackFlow {
        trySend(currentlyOnline())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(currentlyOnline()) }
            override fun onUnavailable() { trySend(false) }
        }
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
