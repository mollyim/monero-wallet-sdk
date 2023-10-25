package im.molly.monero.demo.ui.preview

import im.molly.monero.BlockHeader
import im.molly.monero.BlockchainTime
import im.molly.monero.HashDigest
import im.molly.monero.Ledger
import im.molly.monero.MoneroAmount
import im.molly.monero.MoneroNetwork
import im.molly.monero.PaymentDetail
import im.molly.monero.PublicAddress
import im.molly.monero.Transaction
import im.molly.monero.TxState
import im.molly.monero.UnlockTime
import im.molly.monero.xmr
import java.time.Instant

object PreviewParameterData {
    val network = MoneroNetwork.Mainnet

    val blockHeader = BlockHeader(height = 2999840, epochSecond = 1697792826)

    val recipients =
        listOf(PublicAddress.parse("888tNkZrPN6JsEgekjMnABU4TBzc2Dt29EPAvkRxbANsAnjyPbb3iQ1YBRk1UXcdRsiKc9dhwMVgN5S9cQUiyoogDavup3H"))

    val transactions = listOf(
        Transaction(
            hash = HashDigest("e7a60483591378d536792d070f2bf6ccb7d0666df03b57f485ddaf66899a294b"),
            state = TxState.OnChain(blockHeader),
            network = network,
            timeLock = UnlockTime.Block(BlockchainTime(2999850, Instant.ofEpochSecond(1697792826), network)),
            sent = emptySet(),
            received = emptySet(),
            payments = listOf(PaymentDetail((0.10).xmr, recipients.first())),
            fee = 0.00093088.xmr,
            change = MoneroAmount.ZERO,
        ),
    )

    val ledger = Ledger(
        primaryAddress = PublicAddress.parse("4AYjQM9HoAFNUeC3cvSfgeAN89oMMpMqiByvunzSzhn97cj726rJj3x8hCbH58UnMqQJShczCxbpWRiCJQ3HCUDHLiKuo4T"),
        checkedAt = BlockchainTime(blockHeader = blockHeader, network = network),
        enotes = emptySet(),
        transactions = transactions.associateBy { it.txId },
    )
}
