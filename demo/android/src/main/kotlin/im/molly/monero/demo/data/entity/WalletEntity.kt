package im.molly.monero.demo.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import im.molly.monero.demo.data.model.WalletConfig

@Entity(
    tableName = "wallets",
    indices = [
        Index(value = ["public_address"], unique = true)
    ],
)
data class WalletEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "public_address")
    val publicAddress: String,

    @ColumnInfo(name = "name")
    val name: String = "",
)

fun WalletEntity.asExternalModel() = WalletConfig(
    id = id,
    publicAddress = publicAddress,
    name = name,
    remoteNodes = setOf(),
)

fun WalletConfig.asEntity() = WalletEntity(
    id = id,
    publicAddress = publicAddress,
    name = name
)
