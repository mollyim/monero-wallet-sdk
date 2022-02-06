package im.molly.monero.demo.data

import androidx.room.Database
import androidx.room.RoomDatabase
import im.molly.monero.demo.data.dao.RemoteNodeDao
import im.molly.monero.demo.data.dao.WalletDao
import im.molly.monero.demo.data.entity.RemoteNodeEntity
import im.molly.monero.demo.data.entity.WalletEntity
import im.molly.monero.demo.data.entity.WalletRemoteNodeXRef

private const val DATABASE_VERSION = 1

@Database(
    entities = [
        WalletEntity::class,
        RemoteNodeEntity::class,
        WalletRemoteNodeXRef::class,
//        TransactionEntity::class,
    ],
    version = DATABASE_VERSION,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun remoteNodeDao(): RemoteNodeDao
    abstract fun walletDao(): WalletDao
}
