package com.example.secureflow.net

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.SocketFactory

class NonVpnSocketFactory(private val connectivityManager: ConnectivityManager) : SocketFactory() {

    private fun getNonVpnNetwork(): Network? {
        return connectivityManager.allNetworks.firstOrNull { network ->
            val caps = connectivityManager.getNetworkCapabilities(network)
            caps != null && !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        }
    }

    private fun bindToNonVpn(socket: Socket): Socket {
        val network = getNonVpnNetwork() ?: throw IOException("No non-VPN network available")
        network.bindSocket(socket)
        return socket
    }

    override fun createSocket(): Socket {
        return bindToNonVpn(Socket())
    }

    override fun createSocket(host: String?, port: Int): Socket {
        val socket = bindToNonVpn(Socket())
        socket.connect(InetSocketAddress(host, port))
        return socket
    }

    override fun createSocket(address: InetAddress?, port: Int): Socket {
        val socket = bindToNonVpn(Socket())
        socket.connect(InetSocketAddress(address, port))
        return socket
    }

    override fun createSocket(
        host: String?,
        port: Int,
        localAddress: InetAddress?,
        localPort: Int
    ): Socket {
        val socket = bindToNonVpn(Socket())
        socket.bind(InetSocketAddress(localAddress, localPort))
        socket.connect(InetSocketAddress(host, port))
        return socket
    }

    override fun createSocket(
        address: InetAddress?,
        port: Int,
        localAddress: InetAddress?,
        localPort: Int
    ): Socket {
        val socket = bindToNonVpn(Socket())
        socket.bind(InetSocketAddress(localAddress, localPort))
        socket.connect(InetSocketAddress(address, port))
        return socket
    }
}
