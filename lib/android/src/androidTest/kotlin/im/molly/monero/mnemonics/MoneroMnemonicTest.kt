package im.molly.monero.mnemonics

import com.google.common.truth.Truth.assertThat
import im.molly.monero.parseHex
import org.junit.Test

class MoneroMnemonicTest {

    data class TestCase(val entropy: String, val words: String, val language: String)

    private val testVector = listOf(
        TestCase(
            entropy = "3b094ca7218f175e91fa2402b4ae239a2fe8262792a3e718533a1a357a1e4109",
            words = "tavern judge beyond bifocals deepest mural onward dummy eagle diode gained vacation rally cause firm idled jerseys moat vigilant upload bobsled jobs cunning doing jobs",
            language = "en",
        ),
    )

    @Test
    fun validateKnownMnemonics() {
        testVector.forEach {
            validateMnemonicGeneration(it)
            validateEntropyRecovery(it)
        }
    }

    private fun validateMnemonicGeneration(testCase: TestCase) {
        val mnemonicCode = MoneroMnemonic.generateMnemonic(testCase.entropy.parseHex())
        assertMnemonicCode(mnemonicCode, testCase)
    }

    private fun validateEntropyRecovery(testCase: TestCase) {
        val mnemonicCode = MoneroMnemonic.recoverEntropy(testCase.words)
        assertThat(mnemonicCode).isNotNull()
        assertMnemonicCode(mnemonicCode!!, testCase)
    }

    private fun assertMnemonicCode(mnemonicCode: MnemonicCode, testCase: TestCase) {
        with(mnemonicCode) {
            assertThat(entropy).isEqualTo(testCase.entropy.parseHex())
            assertThat(String(words)).isEqualTo(testCase.words)
            assertThat(locale.language).isEqualTo(testCase.language)
        }
    }
}
