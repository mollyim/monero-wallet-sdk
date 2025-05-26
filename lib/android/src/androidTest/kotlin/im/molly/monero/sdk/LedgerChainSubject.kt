package im.molly.monero.sdk

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout

class LedgerChainSubject private constructor(
    metadata: FailureMetadata,
    private val actual: List<Ledger>,
) : Subject(metadata, actual) {

    companion object {
        fun assertThat(ledgerChain: List<Ledger>): LedgerChainSubject {
            return assertAbout(factory).that(ledgerChain)
        }

        private val factory = Factory { metadata, actual: List<Ledger> ->
            LedgerChainSubject(metadata, actual)
        }
    }

    fun hasValidWalletHistory() {
        isNotEmpty()
        hasStableOrGrowingAccountSets()
        hasStableOrGrowingTransactionSets()
        hasStableOrGrowingKeyImageSets()
        allPublicAddressesMatch()
    }

    fun isNotEmpty() {
        check("ledgers").that(actual).isNotEmpty()
    }

    fun hasStableOrGrowingAccountSets() {
        actual.ledgerTransitionsByHeight { step, prev, next ->
            val prevAccByIndex = prev.indexedAccounts.associateBy { it.accountIndex }
            val nextAccByIndex = next.indexedAccounts.associateBy { it.accountIndex }

            prevAccByIndex.forEach { (accIndex, prevAccount) ->
                val nextAccount = nextAccByIndex[accIndex]
                val subjectPath = "Ledger[$step → ${step + 1}].indexedAccounts[$accIndex]"

                check(subjectPath).that(nextAccount).isNotNull()

                if (nextAccount != null) {
                    val missingAddresses =
                        prevAccount.addresses.toSet() - nextAccount.addresses.toSet()
                    check("missing addresses in $subjectPath").that(missingAddresses).isEmpty()
                }
            }
        }
    }

    fun hasStableOrGrowingTransactionSets() {
        actual.ledgerTransitionsByHeight { step, prev, next ->
            val missingTxIds = prev.transactionById.keys - next.transactionById.keys
            val subjectPath = "Ledger[$step → ${step + 1}].transactionById"

            check("$subjectPath: missing transaction set").that(missingTxIds).isEmpty()
        }
    }

    fun hasStableOrGrowingKeyImageSets() {
        actual.ledgerTransitionsByHeight { step, prev, next ->
            val missingKI = prev.keyImages - next.keyImages
            val subjectPath = "Ledger[$step → ${step + 1}].keyImages"

            check("$subjectPath: missing key images: $missingKI").that(missingKI).isEmpty()
        }
    }

    fun allPublicAddressesMatch() {
        val addresses = actual.map { it.publicAddress }.distinct()
        check("publicAddresses").that(addresses).hasSize(1)
    }

    private fun List<Ledger>.ledgerTransitionsByHeight(
        action: (step: Int, prev: Ledger, next: Ledger) -> Unit
    ) {
        this.sortedBy { it.checkedAt.height }
            .zipWithNext()
            .forEachIndexed { step, (prev, next) -> action(step, prev, next) }
    }
}
