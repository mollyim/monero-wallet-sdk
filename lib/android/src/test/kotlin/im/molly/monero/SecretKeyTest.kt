package im.molly.monero

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.random.Random

class SecretKeyTest {

    @Test
    fun `secret keys are 256 bits`() {
        for (size in 0..64) {
            val secret = Random.nextBytes(size)
            if (size == 32) {
                assertThat(SecretKey(secret).bytes).hasLength(size)
            } else {
                assertThrows(RuntimeException::class.java) { SecretKey(secret) }
            }
        }
    }

    @Test
    fun `secret keys cannot be zero`() {
        assertThrows(RuntimeException::class.java) { SecretKey(ByteArray(32)).bytes }
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
        assertThrows(RuntimeException::class.java) { key.bytes }
    }

    @Test
    fun `two keys with same secret are the same`() {
        val secret = Random.nextBytes(32)
        val anotherSecret = Random.nextBytes(32)

        val key = SecretKey(secret)
        val sameKey = SecretKey(secret)
        val anotherKey = SecretKey(anotherSecret)

        assertThat(key).isEqualTo(sameKey)
        assertThat(sameKey).isNotEqualTo(anotherKey)
        assertThat(anotherKey).isNotEqualTo(key)
    }

    @Test
    fun `randomly generated keys are distinct`() {
        val times = 100_000
        val randomKeys = generateSequence { randomSecretKey() }.take(times).toSet()

        assertThat(randomKeys).hasSize(times)
    }
}
