package im.molly.monero.demo.data

import im.molly.monero.demo.data.model.WalletConfig
import im.molly.monero.sdk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import java.util.*
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
                val configFlow = getWalletConfig(walletId)
                val config = configFlow.first()
                val userSettings = settingsRepository.getUserSettings().first()
                val httpClient = sharedHttpClient.newBuilder()
                    .proxy(userSettings.activeProxy)
                    .build()
                val wallet = moneroSdkClient.openWallet(
                    network = MoneroNetwork.of(config.publicAddress),
                    filename = config.filename,
                    remoteNodes = configFlow.map {
                        it.remoteNodes.map { node ->
                            RemoteNode(
                                url = node.uri.toString(),
                                network = node.network,
                                username = node.username,
                                password = node.password,
                            )
                        }
                    },
                    httpClient = httpClient,
                )
                check(config.publicAddress == wallet.publicAddress.address) {
                    "primary address mismatch"
                }
                wallet
            }
        }.await()
    }

    fun getWalletIdList() = walletDataSource.readWalletIdList()

    fun getMoneroNodeClients(): Flow<List<MoneroNodeClient>> =
        getWalletIdList().map { it.mapNotNull { walletId -> getWallet(walletId).moneroNodeClient } }

    fun getWalletConfig(walletId: Long) = walletDataSource.readWalletConfig(walletId)

    fun getLedger(walletId: Long): Flow<Ledger> = flow {
        emitAll(getWallet(walletId).ledger())
    }

    fun getTransaction(walletId: Long, txId: String): Flow<Transaction?> =
        getLedger(walletId).map { it.transactionById[txId] }

    suspend fun createTransfer(walletId: Long, transferRequest: TransferRequest): PendingTransfer {
        return getWallet(walletId).createTransfer(transferRequest)
    }

    suspend fun addWallet(
        moneroNetwork: MoneroNetwork,
        name: String,
        remoteNodeIds: List<Long>,
    ): Pair<Long, MoneroWallet> {
        val uniqueFilename = UUID.randomUUID().toString()
        val wallet = moneroSdkClient.createWallet(moneroNetwork, uniqueFilename)
        val walletId = walletDataSource.createWalletConfig(
            publicAddress = wallet.publicAddress.address,
            filename = uniqueFilename,
            name = name,
            remoteNodeIds = remoteNodeIds,
        )
        return walletId to wallet
    }

    suspend fun restoreWallet(
        moneroNetwork: MoneroNetwork,
        name: String,
        remoteNodeIds: List<Long>,
        secretSpendKey: SecretKey,
        restorePoint: RestorePoint,
    ): Pair<Long, MoneroWallet> {
        val uniqueFilename = UUID.randomUUID().toString()
        val wallet = moneroSdkClient.restoreWallet(
            moneroNetwork,
            uniqueFilename,
            secretSpendKey,
            restorePoint,
        )
        val walletId = walletDataSource.createWalletConfig(
            publicAddress = wallet.publicAddress.address,
            filename = uniqueFilename,
            name = name,
            remoteNodeIds = remoteNodeIds,
        )
        return walletId to wallet
    }

    suspend fun updateWalletConfig(walletConfig: WalletConfig) =
        walletDataSource.updateWalletConfig(walletConfig)
}
