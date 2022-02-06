package im.molly.monero.demo.data.dao

import androidx.room.*
import im.molly.monero.demo.data.entity.PopulatedWallet
import im.molly.monero.demo.data.entity.WalletEntity
import im.molly.monero.demo.data.entity.WalletRemoteNodeXRef
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Transaction
    @Query(
        value = """
        SELECT * FROM wallets
        WHERE id = :id
    """
    )
    fun findById(id: Long): Flow<PopulatedWallet>

    @Query(
        value = """
        SELECT id FROM wallets
        """
    )
    fun findAllIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(wallet: WalletEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRemoteNodeXRefEntities(walletRemoteNodeXRefs: List<WalletRemoteNodeXRef>)

    @Delete
    suspend fun delete(wallet: WalletEntity)
}
