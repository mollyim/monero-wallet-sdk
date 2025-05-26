package im.molly.monero.demo.data.entity

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import im.molly.monero.demo.data.model.RemoteNode
import im.molly.monero.sdk.MoneroNetwork

@Entity(
    tableName = "remote_nodes",
    indices = [
        Index("net_type")
    ]
)
data class RemoteNodeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "net_type")
    val networkId: Int,

    @ColumnInfo(name = "uri")
    val uri: String,

    @ColumnInfo(name = "user")
    val username: String,

    @ColumnInfo(name = "pwd")
    val password: String,
)

fun RemoteNodeEntity.asExternalModel() = RemoteNode(
    id = id,
    network = MoneroNetwork.fromId(networkId),
    uri = Uri.parse(uri),
    username = username,
    password = password,
)

fun RemoteNode.asEntity() = RemoteNodeEntity(
    id = id ?: 0,
    networkId = network.id,
    uri = uri.toString(),
    username = username,
    password = password,
)
