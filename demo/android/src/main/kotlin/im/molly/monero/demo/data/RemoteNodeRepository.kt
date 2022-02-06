package im.molly.monero.demo.data

import im.molly.monero.demo.data.dao.RemoteNodeDao
import im.molly.monero.demo.data.entity.*
import im.molly.monero.demo.data.model.RemoteNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RemoteNodeRepository(
    private val remoteNodeDao: RemoteNodeDao,
) {
    fun getRemoteNode(remoteNodeId: Long): Flow<RemoteNode> =
        remoteNodeDao.findById(remoteNodeId).map { it.asExternalModel() }

    fun getRemoteNodes(remoteNodeIds: List<Long>): Flow<List<RemoteNode>> =
        remoteNodeDao.findByIds(remoteNodeIds).map { it.map(RemoteNodeEntity::asExternalModel) }

    fun getAllRemoteNodes(filterNetworkIds: Set<Int> = emptySet()): Flow<List<RemoteNode>> =
        if (filterNetworkIds.isEmpty()) {
            remoteNodeDao.findAll()
        } else {
            remoteNodeDao.findByNetworkIds(filterNetworkIds)
        }.map { it.map(RemoteNodeEntity::asExternalModel) }

    suspend fun addOrUpdateRemoteNode(remoteNode: RemoteNode) =
        remoteNodeDao.upsert(remoteNode.asEntity())

    suspend fun deleteRemoteNode(remoteNode: RemoteNode) =
        remoteNodeDao.delete(remoteNode.asEntity())
}
