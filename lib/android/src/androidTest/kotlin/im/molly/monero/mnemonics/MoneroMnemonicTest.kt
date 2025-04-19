package im.molly.monero.mnemonics

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class MoneroMnemonicTest {

    @OptIn(ExperimentalStdlibApi::class)
    data class TestCase(val key: String, val words: String, val language: String) {
        val entropy = key.hexToByteArray()
    }

    private val testCases = listOf(
        TestCase(
            key = "3b094ca7218f175e91fa2402b4ae239a2fe8262792a3e718533a1a357a1e4109",
            words = "tavern judge beyond bifocals deepest mural onward dummy eagle diode gained vacation rally cause firm idled jerseys moat vigilant upload bobsled jobs cunning doing jobs",
            language = "en",
        ),
    )

    @Test
    fun knownMnemonics() {
        testCases.forEach {
            validateMnemonicGeneration(it)
            validateEntropyRecovery(it)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyEntropy() {
        MoneroMnemonic.generateMnemonic(ByteArray(0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidEntropy() {
        MoneroMnemonic.generateMnemonic(ByteArray(2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyWords() {
        MoneroMnemonic.recoverEntropy("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidLanguage() {
        MoneroMnemonic.generateMnemonic(ByteArray(32), Locale("ZZ"))
    }

    private fun validateMnemonicGeneration(testCase: TestCase) {
        val mnemonicCode =
            MoneroMnemonic.generateMnemonic(testCase.entropy, Locale(testCase.language))
        assertMnemonicCode(mnemonicCode, testCase)
    }

    private fun validateEntropyRecovery(testCase: TestCase) {
        val mnemonicCode = MoneroMnemonic.recoverEntropy(testCase.words)
        assertMnemonicCode(mnemonicCode, testCase)
    }

    private fun assertMnemonicCode(mnemonicCode: MnemonicCode?, testCase: TestCase) {
        assertThat(mnemonicCode).isNotNull()
        with(mnemonicCode!!) {
            assertThat(entropy).isEqualTo(testCase.entropy)
            assertThat(String(words)).isEqualTo(testCase.words)
            assertThat(locale.language).isEqualTo(testCase.language)
        }
    }
}
