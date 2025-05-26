package im.molly.monero.sdk

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class BalanceTest {

    @Test
    fun `confirmed and total amounts are sum of time-locked values`() {
        val balance = Balance(
            lockableAmounts = listOf(1.xmr, 2.xmr, 3.xmr).map { it.unlocked() },
        )

        assertThat(balance.confirmedAmount).isEqualTo(6.xmr)
        assertThat(balance.totalAmount).isEqualTo(6.xmr)
    }

    @Test
    fun `total amount includes pending`() {
        val balance = Balance(
            lockableAmounts = listOf(2.xmr.unlocked()),
            pendingAmount = 4.xmr,
        )

        assertThat(balance.totalAmount).isEqualTo(6.xmr)
    }

    @Test
    fun `empty balance returns zero amounts`() {
        assertThat(Balance.EMPTY.totalAmount).isEqualTo(0.xmr)
    }

    @Test
    fun `excludes locked amounts from unlocked sum`() {
        val current = BlockchainTime(100, Instant.now(), Mainnet)

        val allUnlocked = Balance(
            listOf(
                TimeLocked(2.xmr, Mainnet.unlockAtBlock(50)),
                TimeLocked(3.xmr, Mainnet.unlockAtBlock(50))
            )
        )
        assertThat(allUnlocked.unlockedAmountAt(current)).isEqualTo(5.xmr)

        val allLocked = Balance(
            listOf(
                TimeLocked(2.xmr, Mainnet.unlockAtBlock(150)),
                TimeLocked(3.xmr, Mainnet.unlockAtBlock(200))
            )
        )
        assertThat(allLocked.unlockedAmountAt(current)).isEqualTo(0.xmr)

        val partial = Balance(
            listOf(
                TimeLocked(1.xmr, Mainnet.unlockAtBlock(50)),
                TimeLocked(2.xmr, Mainnet.unlockAtBlock(100)),
                TimeLocked(5.xmr, Mainnet.unlockAtBlock(150)),
            )
        )
        assertThat(partial.unlockedAmountAt(current)).isEqualTo(3.xmr)
    }
}
