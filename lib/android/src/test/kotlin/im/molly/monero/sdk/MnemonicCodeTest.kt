package im.molly.monero.sdk

import com.google.common.truth.Truth.assertThat
import im.molly.monero.sdk.mnemonics.MnemonicCode
import org.junit.Test
import java.nio.CharBuffer
import java.util.Locale
import kotlin.random.Random
import kotlin.test.assertFailsWith

class MnemonicCodeTest {

    private fun randomEntropy(size: Int = 32): ByteArray = Random.nextBytes(size)

    private fun charBufferOf(str: String): CharBuffer = CharBuffer.wrap(str.toCharArray())

    @Test
    fun `mnemonic copies entropy and words`() {
        val entropy = randomEntropy()
        val words = charBufferOf("arbre soleil maison")
        val locale = Locale.FRANCE

        val mnemonic = MnemonicCode(entropy, words, locale)

        assertThat(mnemonic.entropy).isEqualTo(entropy)
        assertThat(mnemonic.words).isEqualTo(words.array())
        assertThat(mnemonic.locale).isEqualTo(locale)

        entropy.fill(0)
        words.put("modified".toCharArray())

        assertThat(mnemonic.entropy).isNotEqualTo(entropy)
        assertThat(mnemonic.words).isNotEqualTo(words.array())
    }

    @Test
    fun `destroyed mnemonic code zeroes entropy and words`() {
        val entropy = randomEntropy()
        val words = charBufferOf("test mnemonic")

        val mnemonic = MnemonicCode(entropy, words)

        mnemonic.destroy()

        assertThat(mnemonic.destroyed).isTrue()
        assertThat(mnemonic.isNonZero).isFalse()
        assertFailsWith<IllegalStateException> { mnemonic.words }
        assertFailsWith<IllegalStateException> { mnemonic.entropy }
    }

    @Test
    fun `two mnemonics with same entropy are equal`() {
        val entropy = randomEntropy()
        val words = charBufferOf("test mnemonic")
        val locale = Locale.ENGLISH

        val mnemonic = MnemonicCode(entropy, words, locale)
        val sameMnemonic = MnemonicCode(entropy, words, locale)
        val differentMnemonic = MnemonicCode(randomEntropy(), words, locale)

        assertThat(mnemonic).isEqualTo(sameMnemonic)
        assertThat(differentMnemonic).isNotEqualTo(mnemonic)
    }

    @Test
    fun `iterator correctly iterates words`() {
        val words = charBufferOf("word1 word2 word3")
        val mnemonic = MnemonicCode(randomEntropy(), words)

        val iteratedWords = mnemonic.map { String(it) }

        assertThat(iteratedWords).containsExactly("word1", "word2", "word3").inOrder()
    }

    @Test
    fun `calling next on iterator without checking hasNext throws exception`() {
        val words = charBufferOf("test mnemonic")
        val mnemonic = MnemonicCode(randomEntropy(), words)
        val iterator = mnemonic.iterator()

        iterator.next()
        iterator.next()

        assertFailsWith<NoSuchElementException> { iterator.next() }
    }

    @Test
    fun `mnemonics are not equal to their destroyed versions`() {
        val entropy = randomEntropy()
        val words = charBufferOf("test mnemonic")

        val mnemonic = MnemonicCode(entropy, words)
        val destroyed = MnemonicCode(entropy, words).also { it.destroy() }

        assertThat(mnemonic).isNotEqualTo(destroyed)
    }

    @Test
    fun `destroyed mnemonics are equal`() {
        val destroyed1 = MnemonicCode(randomEntropy(), charBufferOf("word1")).also { it.destroy() }
        val destroyed2 = MnemonicCode(randomEntropy(), charBufferOf("word2")).also { it.destroy() }

        assertThat(destroyed1).isEqualTo(destroyed2)
    }
}
