package im.molly.monero.sdk

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout
import kotlinx.coroutines.flow.first

class MoneroWalletSubject private constructor(
    metadata: FailureMetadata,
    private val actual: MoneroWallet,
) : Subject(metadata, actual) {

    companion object {
        fun assertThat(wallet: MoneroWallet): MoneroWalletSubject {
            return assertAbout(factory).that(wallet)
        }

        private val factory = Factory { metadata, actual: MoneroWallet ->
            MoneroWalletSubject(metadata, actual)
        }
    }

    suspend fun matchesStateOf(expected: MoneroWallet) {
        with(actual) {
            check("publicAddress").that(publicAddress).isEqualTo(expected.publicAddress)
            check("ledger").that(ledger().first()).isEqualTo(expected.ledger().first())
        }
    }
}
