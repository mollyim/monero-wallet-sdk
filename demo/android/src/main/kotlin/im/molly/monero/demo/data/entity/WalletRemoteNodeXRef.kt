package im.molly.monero.demo.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "wallet_remote_nodes",
    primaryKeys = ["wallet_id", "remote_node_id"],
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["wallet_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RemoteNodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["remote_node_id"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = ["wallet_id"]),
        Index(value = ["remote_node_id"]),
    ],
)
data class WalletRemoteNodeXRef(
    @ColumnInfo(name = "wallet_id")
    val walletId: Long,

    @ColumnInfo(name = "remote_node_id")
    val remoteNodeId: Long,
)
