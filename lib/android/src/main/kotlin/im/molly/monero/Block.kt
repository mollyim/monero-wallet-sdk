package im.molly.monero

import im.molly.monero.internal.constants.CRYPTONOTE_MAX_BLOCK_NUMBER

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
    val timestamp: Long,
) {
    companion object {
        const val MAX_HEIGHT = CRYPTONOTE_MAX_BLOCK_NUMBER - 1
    }
}

fun isBlockHeightInRange(height: Long) = !(height < 0 || height > BlockHeader.MAX_HEIGHT)

fun isBlockHeightInRange(height: Int) = isBlockHeightInRange(height.toLong())
