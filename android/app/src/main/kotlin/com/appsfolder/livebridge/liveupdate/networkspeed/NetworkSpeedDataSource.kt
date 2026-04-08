package com.appsfolder.livebridge.liveupdate.networkspeed

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TrafficStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap

data class NetworkTrafficData(
    val rxBytes: Long,
    val txBytes: Long,
)

data class NetworkSpeedSample(
    val downloadBytesPerSecond: Long = 0L,
    val uploadBytesPerSecond: Long = 0L,
) {
    val totalBytesPerSecond: Long
        get() = downloadBytesPerSecond + uploadBytesPerSecond
}

class NetworkSpeedDataSource(context: Context) : Closeable {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val validInterfaces = ConcurrentHashMap<Network, String>()
    private var callbackRegistered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            updateNetwork(
                network = network,
                capabilities = networkCapabilities,
                linkProperties = connectivityManager.getLinkProperties(network),
            )
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            super.onLinkPropertiesChanged(network, linkProperties)
            updateNetwork(
                network = network,
                capabilities = connectivityManager.getNetworkCapabilities(network),
                linkProperties = linkProperties,
            )
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            validInterfaces.remove(network)
        }
    }

    init {
        registerCallback()
        seedExistingNetworks()
    }

    suspend fun getTrafficData(): NetworkTrafficData = withContext(Dispatchers.Default) {
        var totalRx = 0L
        var totalTx = 0L
        val interfaceNames = validInterfaces.values.toSet()

        interfaceNames.forEach { ifaceName ->
            val rxBytes = TrafficStats.getRxBytes(ifaceName)
            val txBytes = TrafficStats.getTxBytes(ifaceName)
            if (rxBytes != TrafficStats.UNSUPPORTED.toLong()) {
                totalRx += rxBytes.coerceAtLeast(0L)
            }
            if (txBytes != TrafficStats.UNSUPPORTED.toLong()) {
                totalTx += txBytes.coerceAtLeast(0L)
            }
        }

        NetworkTrafficData(rxBytes = totalRx, txBytes = totalTx)
    }

    override fun close() {
        if (!callbackRegistered) {
            return
        }
        runCatching {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
        callbackRegistered = false
        validInterfaces.clear()
    }

    private fun registerCallback() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()

        runCatching {
            connectivityManager.registerNetworkCallback(request, networkCallback)
            callbackRegistered = true
        }
    }

    private fun seedExistingNetworks() {
        connectivityManager.allNetworks.forEach { network ->
            updateNetwork(
                network = network,
                capabilities = connectivityManager.getNetworkCapabilities(network),
                linkProperties = connectivityManager.getLinkProperties(network),
            )
        }
    }

    private fun updateNetwork(
        network: Network,
        capabilities: NetworkCapabilities?,
        linkProperties: LinkProperties?,
    ) {
        if (capabilities == null || linkProperties == null) {
            validInterfaces.remove(network)
            return
        }

        val isVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        val isPhysical =
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        val interfaceName = linkProperties.interfaceName

        if (isVpn || !isPhysical || interfaceName.isNullOrBlank()) {
            validInterfaces.remove(network)
            return
        }

        validInterfaces[network] = interfaceName
    }
}
