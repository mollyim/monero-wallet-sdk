package im.molly.monero.demo.data.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class PopulatedWallet(
    @Embedded
    val wallet: WalletEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = WalletRemoteNodeXRef::class,
            parentColumn = "wallet_id",
            entityColumn = "remote_node_id",
        )
    )
    val remoteNodes: Set<RemoteNodeEntity>,
)

fun PopulatedWallet.asExternalModel() = wallet.asExternalModel().copy(
    remoteNodes = remoteNodes.map(RemoteNodeEntity::asExternalModel).toSet()
)
