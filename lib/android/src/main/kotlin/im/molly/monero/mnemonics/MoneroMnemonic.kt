package im.molly.monero.mnemonics

import im.molly.monero.CalledByNative
import im.molly.monero.internal.NativeLoader
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.util.Locale

object MoneroMnemonic {
    init {
        NativeLoader.loadMnemonicsLibrary()
    }

    // Supported languages based on mnemonics/electrum-words.cpp
    val supportedLanguages = mapOf(
        "ang" to "English (old)",
        "de" to "German",
        "en" to "English",
        "eo" to "Esperanto",
        "es" to "Spanish",
        "fr" to "French",
        "it" to "Italian",
        "ja" to "Japanese",
        "jbo" to "Lojban",
        "nl" to "Dutch",
        "pt" to "Portuguese",
        "ru" to "Russian",
        "zh" to "Chinese (simplified)",
    )

    fun generateMnemonic(entropy: ByteArray, locale: Locale = Locale.US): MnemonicCode {
        require(entropy.isNotEmpty()) { "Entropy must not be empty" }
        require(entropy.size % 4 == 0) { "Entropy size must be a multiple of 4" }

        val language = supportedLanguages[locale.language]
            ?: throw IllegalArgumentException("Invalid locale: $locale")

        return requireNotNull(nativeElectrumWordsGenerateMnemonic(entropy, language))
    }

    fun recoverEntropy(words: CharArray): MnemonicCode? =
        recoverEntropy(CharBuffer.wrap(words))

    fun recoverEntropy(words: CharSequence): MnemonicCode? =
        recoverEntropy(CharBuffer.wrap(words))

    private fun recoverEntropy(words: CharBuffer): MnemonicCode? {
        require(words.isNotEmpty()) { "Input words must not be empty" }

        val byteBuffer = StandardCharsets.UTF_8.encode(words)
        val wordsBytes = ByteArray(byteBuffer.remaining())
        byteBuffer.get(wordsBytes)
        byteBuffer.array().fill(0)

        return try {
            nativeElectrumWordsRecoverEntropy(wordsBytes)
        } finally {
            wordsBytes.fill(0)
        }
    }

    @CalledByNative
    @JvmStatic
    private fun buildMnemonicFromJNI(
        entropy: ByteArray,
        wordsBytes: ByteArray,
        language: String,
    ): MnemonicCode {
        val byteBuffer = ByteBuffer.wrap(wordsBytes)
        val words = StandardCharsets.UTF_8.decode(byteBuffer)
        val languageCode = supportedLanguages.entries.first { it.value == language }.key

        return try {
            MnemonicCode(entropy, words, Locale(languageCode))
        } finally {
            words.array().fill(0.toChar())
        }
    }
}

private external fun nativeElectrumWordsGenerateMnemonic(
    entropy: ByteArray,
    language: String,
): MnemonicCode?

private external fun nativeElectrumWordsRecoverEntropy(
    source: ByteArray,
): MnemonicCode?
