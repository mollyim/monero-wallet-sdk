package im.molly.monero.demo.data.model

data class WalletConfig(
    val id: Long,
    val publicAddress: String,
    val filename: String,
    val name: String,
    val remoteNodes: Set<RemoteNode>,
)
