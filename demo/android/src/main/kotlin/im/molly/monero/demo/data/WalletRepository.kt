package im.molly.monero.demo.data

import im.molly.monero.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class WalletRepository(
    private val moneroSdkClient: MoneroSdkClient,
    private val walletDataSource: WalletDataSource,
    private val externalScope: CoroutineScope,
) {
    private val walletIdMap = ConcurrentHashMap<Long, Deferred<MoneroWallet>>()

    suspend fun getWallet(walletId: Long): MoneroWallet {
        return walletIdMap.computeIfAbsent(walletId) {
            externalScope.async {
                val config = getWalletConfig(walletId).first()
                val wallet = moneroSdkClient.openWallet(
                    publicAddress = config.publicAddress,
                    remoteNodeSelector = object : RemoteNodeSelector {
                        override fun select(): RemoteNode? {
                            return config.remoteNodes.firstOrNull()?.let {
                                RemoteNode(
                                    uri = it.uri,
                                    username = it.username,
                                    password = it.password,
                                )
                            }
                        }

                        override fun connectFailed(remoteNode: RemoteNode, ioe: IOException) {
                            TODO("Not yet implemented")
                        }
                    }
                )
                wallet
            }
        }.await()
    }

    fun getWalletIdList() = walletDataSource.getWalletIdList()

    fun getWalletConfig(walletId: Long) = walletDataSource.loadWalletConfig(walletId)

    fun getLedger(walletId: Long): Flow<Ledger> = flow {
        emitAll(getWallet(walletId).ledger())
    }

    suspend fun addWallet(
        moneroNetwork: MoneroNetwork,
        name: String,
        remoteNodeIds: List<Long>,
    ): Pair<Long, IWallet> {
        val wallet = moneroSdkClient.createWallet(moneroNetwork)
        val walletId = walletDataSource.storeWalletConfig(
            publicAddress = wallet.publicAddress,
            name = name,
            remoteNodeIds = remoteNodeIds,
        )
        return walletId to wallet
    }
}
