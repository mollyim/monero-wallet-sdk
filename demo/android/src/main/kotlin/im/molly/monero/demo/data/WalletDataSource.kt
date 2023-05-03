package im.molly.monero.demo.data

import im.molly.monero.demo.data.dao.WalletDao
import im.molly.monero.demo.data.entity.WalletEntity
import im.molly.monero.demo.data.entity.WalletRemoteNodeXRef
import im.molly.monero.demo.data.entity.asEntity
import im.molly.monero.demo.data.entity.asExternalModel
import im.molly.monero.demo.data.model.WalletConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WalletDataSource(
    private val walletDao: WalletDao,
) {
    fun readWalletIdList(): Flow<List<Long>> = walletDao.findAllIds()

    fun readWalletConfig(walletId: Long): Flow<WalletConfig> =
        walletDao.findById(walletId).map { it.asExternalModel() }

    suspend fun createWalletConfig(
        publicAddress: String,
        filename: String,
        name: String,
        remoteNodeIds: List<Long>,
    ): Long {
        val walletId = walletDao.insert(
            WalletEntity(
                publicAddress = publicAddress,
                filename = filename,
                name = name,
            )
        )
        val walletRemoteNodeXRef = remoteNodeIds.map { remoteNodeId ->
            WalletRemoteNodeXRef(
                walletId = walletId,
                remoteNodeId = remoteNodeId
            )
        }
        walletDao.insertRemoteNodeXRefEntities(walletRemoteNodeXRef)
        return walletId
    }

    suspend fun updateWalletConfig(walletConfig: WalletConfig) {
        walletDao.update(walletConfig.asEntity())
    }
}
