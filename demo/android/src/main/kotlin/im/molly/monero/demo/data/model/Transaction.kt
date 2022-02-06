package im.molly.monero.demo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Transaction(
    @PrimaryKey val uid: Int,
)
