package im.molly.monero.demo.data.model

import java.net.InetSocketAddress
import java.net.Proxy
import java.net.SocketAddress

data class SocksProxy(val socketAddress: SocketAddress) : Proxy(Type.SOCKS, socketAddress)

fun String.toSocketAddress(): SocketAddress =
    try {
        val index = lastIndexOf(':')
        val host = substring(0, index)
        val port = substring(index + 1).toInt()
        InetSocketAddress.createUnresolved(host, port)
    } catch (t: IndexOutOfBoundsException) {
        throw IllegalArgumentException()
    }
