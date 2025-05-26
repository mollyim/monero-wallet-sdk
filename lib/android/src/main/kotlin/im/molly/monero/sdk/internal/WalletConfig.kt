package im.molly.monero.sdk.internal

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class WalletConfig(
    val networkId: Int,
) : Parcelable
