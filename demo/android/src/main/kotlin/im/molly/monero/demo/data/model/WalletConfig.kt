package im.molly.monero.demo.data.model

import im.molly.monero.MoneroNetwork

data class WalletConfig(
    val id: Long,
    val publicAddress: String,
    val network: MoneroNetwork = MoneroNetwork.of(publicAddress),
    val name: String,
    val remoteNodes: Set<RemoteNode>,
)
