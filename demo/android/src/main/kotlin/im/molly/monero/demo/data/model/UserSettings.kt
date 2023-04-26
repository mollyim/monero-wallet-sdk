package im.molly.monero.demo.data.model

import java.net.Proxy

data class UserSettings(
    val socksProxy: SocksProxy?,
) {
    val activeProxy: Proxy = socksProxy ?: Proxy.NO_PROXY
}
