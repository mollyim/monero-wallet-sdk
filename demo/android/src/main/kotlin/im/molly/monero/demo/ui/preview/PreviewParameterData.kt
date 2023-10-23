package im.molly.monero.demo.ui.preview

import im.molly.monero.BlockHeader
import im.molly.monero.BlockchainTime
import im.molly.monero.HashDigest
import im.molly.monero.Ledger
import im.molly.monero.MoneroAmount
import im.molly.monero.PaymentDetail
import im.molly.monero.PublicAddress
import im.molly.monero.Transaction
import im.molly.monero.TxState
import im.molly.monero.xmr

object PreviewParameterData {
    val recipients =
        listOf(PublicAddress.parse("888tNkZrPN6JsEgekjMnABU4TBzc2Dt29EPAvkRxbANsAnjyPbb3iQ1YBRk1UXcdRsiKc9dhwMVgN5S9cQUiyoogDavup3H"))

    val transactions = listOf(
        Transaction(
            hash = HashDigest("e7a60483591378d536792d070f2bf6ccb7d0666df03b57f485ddaf66899a294b"),
            state = TxState.OnChain(BlockHeader(height = 2999840, epochSecond = 1697792826)),
            timeLock = BlockchainTime.Block(2999850),
            sent = emptySet(),
            received = emptySet(),
            payments = listOf(PaymentDetail((0.10).xmr, recipients.first())),
            fee = 0.00093088.xmr,
            change = MoneroAmount.ZERO,
        ),
    )

    val ledger = Ledger(
        primaryAddress = PublicAddress.parse("4AYjQM9HoAFNUeC3cvSfgeAN89oMMpMqiByvunzSzhn97cj726rJj3x8hCbH58UnMqQJShczCxbpWRiCJQ3HCUDHLiKuo4T"),
        checkedAt = BlockchainTime.Block(2999840),
        enotes = emptySet(),
        transactions = transactions.associateBy { it.txId },
    )
}
