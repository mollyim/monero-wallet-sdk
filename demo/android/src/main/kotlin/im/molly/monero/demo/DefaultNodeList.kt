package im.molly.monero.demo

import im.molly.monero.demo.data.AppDatabase
import im.molly.monero.demo.data.entity.asEntity
import im.molly.monero.demo.data.model.RemoteNode
import im.molly.monero.sdk.MoneroNetwork
import androidx.core.net.toUri

val DefaultNodeList = listOf(
    MoneroNetwork.Mainnet to listOf(
        "http://node.monerodevs.org:18089",
        "http://node.sethforprivacy.com:18089",
        "http://xmr-node.cakewallet.com:18081",
    ),
    MoneroNetwork.Testnet to listOf(
        "http://node2.monerodevs.org:28089",
    ),
)

suspend fun addDefaultRemoteNodes(appDatabase: AppDatabase) {
    val dao = appDatabase.remoteNodeDao()
    val nodes = DefaultNodeList.flatMap { (network, urls) ->
        urls.map { url ->
            RemoteNode(network = network, uri = url.toUri()).asEntity()
        }
    }.toTypedArray()
    dao.upsert(*nodes)
}
