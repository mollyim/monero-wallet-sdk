package im.molly.monero.sdk.internal

import im.molly.monero.sdk.BlockchainTime
import im.molly.monero.sdk.Ledger
import im.molly.monero.sdk.WalletAccount
import im.molly.monero.sdk.findAddressByIndex

internal object LedgerFactory {
    fun createFromTxHistory(
        txList: List<TxInfo>,
        accounts: List<WalletAccount>,
        blockchainTime: BlockchainTime,
    ): Ledger {
        val (txById, enotes) = txList.consolidateTransactions(
            accounts = accounts,
            blockchainContext = blockchainTime,
        )
        val publicAddress = accounts.findAddressByIndex(accountIndex = 0)
        checkNotNull(publicAddress)
        return Ledger(
            publicAddress = publicAddress,
            indexedAccounts = accounts,
            transactionById = txById,
            enoteSet = enotes,
            checkedAt = blockchainTime,
        )
    }
}
