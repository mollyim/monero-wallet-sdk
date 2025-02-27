package im.molly.monero.internal

import im.molly.monero.BlockchainTime
import im.molly.monero.Ledger
import im.molly.monero.WalletAccount
import im.molly.monero.findAddressByIndex

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
