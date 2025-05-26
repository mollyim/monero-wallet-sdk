package im.molly.monero.sdk

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertFailsWith

class SecretKeyTest {

    @Test
    fun `secret keys are 256 bits`() {
        for (size in 0..64) {
            val secret = Random.nextBytes(size)
            if (size == 32) {
                assertThat(SecretKey(secret).bytes).hasLength(size)
            } else {
                assertFailsWith<IllegalArgumentException> { SecretKey(secret) }
            }
        }
    }

    @Test
    fun `secret key copies buffer`() {
        val secretBytes = Random.nextBytes(32)
        val key = SecretKey(secretBytes)

        assertThat(key.bytes).isEqualTo(secretBytes)
        secretBytes.fill(0)
        assertThat(key.bytes).isNotEqualTo(secretBytes)
    }

    @Test
    fun `secret keys cannot be zero`() {
        assertFailsWith<IllegalStateException> { SecretKey(ByteArray(32)).bytes }
    }

    @Test
    fun `when key is destroyed secret is zeroed`() {
        val secretBytes = Random.nextBytes(32)
        val key = SecretKey(secretBytes)

        assertThat(key.destroyed).isFalse()
        assertThat(key.bytes).isEqualTo(secretBytes)

        key.destroy()

        assertThat(key.destroyed).isTrue()
        assertThat(key.isNonZero).isFalse()
        assertFailsWith<IllegalStateException> { key.bytes }
    }

    @Test
    fun `two keys with same secret are equal`() {
        val secret = Random.nextBytes(32)

        val key = SecretKey(secret)
        val sameKey = SecretKey(secret)
        val differentKey = randomSecretKey()

        assertThat(key).isEqualTo(sameKey)
        assertThat(sameKey).isNotEqualTo(differentKey)
        assertThat(differentKey).isNotEqualTo(key)
    }

    @Test
    fun `randomly generated keys are distinct`() {
        val times = 100_000
        val randomKeys = generateSequence { randomSecretKey() }.take(times).toSet()

        assertThat(randomKeys).hasSize(times)
    }

    @Test
    fun `keys are not equal to their destroyed versions`() {
        val secret = Random.nextBytes(32)
        val key = SecretKey(secret)
        val destroyed = SecretKey(secret).also { it.destroy() }

        assertThat(key).isNotEqualTo(destroyed)
    }

    @Test
    fun `destroyed keys are equal`() {
        val destroyed1 = randomSecretKey().also { it.destroy() }
        val destroyed2 = randomSecretKey().also { it.destroy() }

        assertThat(destroyed1).isEqualTo(destroyed2)
    }
}
