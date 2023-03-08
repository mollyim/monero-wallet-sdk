package im.molly.monero.demo.data

import im.molly.monero.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
                val config = getWalletConfig(walletId)
                val wallet = moneroSdkClient.openWallet(
                    publicAddress = config.first().publicAddress,
                    remoteNodes = config.map {
                        it.remoteNodes.map { node ->
                            RemoteNode(
                                uri = node.uri,
                                username = node.username,
                                password = node.password,
                            )
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
