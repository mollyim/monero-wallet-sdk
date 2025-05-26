package im.molly.monero.sdk

import kotlin.test.Test
import kotlin.test.assertFailsWith

class TimeLockedTest {

    private val mainnetUnlockTime = UnlockTime.Block(100, Mainnet)
    private val locked = 1.xmr.lockedUntil(mainnetUnlockTime)

    @Test
    fun `isLocked throws on network mismatch`() {
        assertFailsWith<IllegalArgumentException> {
            locked.isLocked(Stagenet.genesisTime)
        }
    }

    @Test
    fun `timeUntilUnlock throws on network mismatch`() {
        assertFailsWith<IllegalArgumentException> {
            locked.timeUntilUnlock(Stagenet.genesisTime)
        }
    }
}
