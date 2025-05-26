package im.molly.monero.sdk

interface ProtocolInfo {
    val version: Int
    val perByteFee: Boolean
    val feeScaling2021: Boolean
}

data class MoneroReleaseInfo(override val version: Int) : ProtocolInfo {
    override val perByteFee = version >= 8
    override val feeScaling2021 = version >= 15
}
