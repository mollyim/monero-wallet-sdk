package im.molly.monero.demo.data.dao

import androidx.room.*
import im.molly.monero.demo.data.entity.RemoteNodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteNodeDao {
    @Query(
        value = """
        SELECT * FROM remote_nodes
        """
    )
    fun findAll(): Flow<List<RemoteNodeEntity>>

    @Query(
        value = """
        SELECT * FROM remote_nodes
        WHERE id = :id
        """
    )
    fun findById(id: Long): Flow<RemoteNodeEntity>

    @Query(
        value = """
        SELECT * FROM remote_nodes
        WHERE id IN (:ids)
        """
    )
    fun findByIds(ids: List<Long>): Flow<List<RemoteNodeEntity>>

    @Query(
        value = """
        SELECT * FROM remote_nodes
        WHERE net_type IN (:networkIds)
        """
    )
    fun findByNetworkIds(networkIds: Set<Int>): Flow<List<RemoteNodeEntity>>

    @Upsert
    suspend fun upsert(vararg remoteNodes: RemoteNodeEntity)

    @Delete
    suspend fun delete(vararg remoteNodes: RemoteNodeEntity)
}
