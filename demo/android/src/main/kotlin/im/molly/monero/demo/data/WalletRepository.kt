package im.molly.monero.demo.data

import im.molly.monero.*
import im.molly.monero.demo.data.model.WalletConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap

class WalletRepository(
    private val moneroSdkClient: MoneroSdkClient,
    private val walletDataSource: WalletDataSource,
    private val settingsRepository: SettingsRepository,
    private val externalScope: CoroutineScope,
) {
    private val walletIdMap = ConcurrentHashMap<Long, Deferred<MoneroWallet>>()

    private val sharedHttpClient = OkHttpClient.Builder().build()

    suspend fun getWallet(walletId: Long): MoneroWallet {
        return walletIdMap.computeIfAbsent(walletId) {
            externalScope.async {
                val config = getWalletConfig(walletId)
                val userSettings = settingsRepository.getUserSettings().first()
                val httpClient = sharedHttpClient.newBuilder()
                    .proxy(userSettings.activeProxy)
                    .build()
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
                    },
                    httpClient = httpClient,
                )
                wallet
            }
        }.await()
    }

    fun getWalletIdList() = walletDataSource.readWalletIdList()

    fun getRemoteClients(): Flow<List<RemoteNodeClient>> =
        getWalletIdList().map { it.mapNotNull { walletId -> getWallet(walletId).remoteNodeClient } }

    fun getWalletConfig(walletId: Long) = walletDataSource.readWalletConfig(walletId)

    fun getLedger(walletId: Long): Flow<Ledger> = flow {
        emitAll(getWallet(walletId).ledger())
    }

    suspend fun addWallet(
        moneroNetwork: MoneroNetwork,
        name: String,
        remoteNodeIds: List<Long>,
    ): Pair<Long, MoneroWallet> {
        val wallet = moneroSdkClient.createWallet(moneroNetwork)
        val walletId = walletDataSource.createWalletConfig(
            publicAddress = wallet.publicAddress,
            name = name,
            remoteNodeIds = remoteNodeIds,
        )
        return walletId to wallet
    }

    suspend fun saveWallet(wallet: MoneroWallet) =
        moneroSdkClient.saveWallet(wallet)

    suspend fun updateWalletConfig(walletConfig: WalletConfig) =
        walletDataSource.updateWalletConfig(walletConfig)
}
