package im.molly.monero

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout
import java.math.BigDecimal

class LedgerSubject private constructor(
    metadata: FailureMetadata,
    private val actual: Ledger,
) : Subject(metadata, actual) {

    companion object {
        fun assertThat(ledgerChain: Ledger): LedgerSubject {
            return assertAbout(factory).that(ledgerChain)
        }

        private val factory = Factory { metadata, actual: Ledger ->
            LedgerSubject(metadata, actual)
        }
    }

    fun isConsistent() {
        balanceIsNonNegative()
    }

    fun balanceIsNonNegative() {
        actual.indexedAccounts.forEach { account ->
            val accountIndex = account.accountIndex
            val balance = actual.getBalanceForAccount(accountIndex)

            val pending = balance.pendingAmount.xmr
            val confirmed = balance.confirmedAmount.xmr

            check("indexedAccounts[$accountIndex].pendingAmount.xmr").that(pending)
                .isAtLeast(BigDecimal.ZERO)
            check("indexedAccounts[$accountIndex].confirmedAmount.xmr").that(confirmed)
                .isAtLeast(BigDecimal.ZERO)
        }
    }
}
