package im.molly.monero.sdk

import im.molly.monero.sdk.internal.constants.CRYPTONOTE_MAX_BLOCK_NUMBER
import java.time.Instant

data class Block(
    // TODO: val hash: HashDigest,
    val header: BlockHeader,
    val minerRewardTxIndex: Int,
    val txs: Set<String>,
) {
    // TODO: val blockId: String get() = hash.toString()
}

data class BlockHeader(
    val height: Int,
    val epochSecond: Long,
//    val version: ProtocolInfo,
) {
    val timestamp: Instant
        get() = Instant.ofEpochSecond(epochSecond)

    companion object {
        const val MAX_HEIGHT = CRYPTONOTE_MAX_BLOCK_NUMBER - 1
    }
}

fun isBlockHeightInRange(height: Long) = height in 0..BlockHeader.MAX_HEIGHT

fun isBlockHeightInRange(height: Int) = isBlockHeightInRange(height.toLong())

fun isBlockEpochInRange(epochSecond: Long) = epochSecond > BlockHeader.MAX_HEIGHT
