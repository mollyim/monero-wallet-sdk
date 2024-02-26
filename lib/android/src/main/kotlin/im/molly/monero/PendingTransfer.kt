package im.molly.monero

class PendingTransfer internal constructor(
    private val pendingTransfer: IPendingTransfer,
) : AutoCloseable {

    override fun close() = pendingTransfer.close()
}
