package im.molly.monero.mnemonics

import java.io.Closeable
import java.nio.CharBuffer
import java.security.MessageDigest
import java.util.Locale
import javax.security.auth.Destroyable

class MnemonicCode private constructor(
    private val _entropy: ByteArray,
    private val _words: CharArray,
    val locale: Locale,
) : Destroyable, Closeable, Iterable<CharArray> {

    constructor(entropy: ByteArray, words: CharBuffer, locale: Locale) : this(
        entropy.clone(),
        words.array().copyOfRange(words.position(), words.remaining()),
        locale,
    )

    val entropy: ByteArray
        get() = checkNotDestroyed { _entropy.clone() }

    val words: CharArray
        get() = checkNotDestroyed { _words.clone() }

    override fun iterator(): Iterator<CharArray> = object : Iterator<CharArray> {
        private var cursor: Int = 0

        override fun hasNext(): Boolean = checkNotDestroyed { cursor < _words.size }

        override fun next(): CharArray {
            if (!hasNext()) {
                throw NoSuchElementException()
            }

            val endIndex = findNextWordEnd(cursor)
            val currentWord = _words.copyOfRange(cursor, endIndex)
            cursor = endIndex + 1

            return currentWord
        }

        private fun findNextWordEnd(startIndex: Int): Int {
            var endIndex = startIndex
            while (endIndex < _words.size && _words[endIndex] != ' ') {
                endIndex++
            }
            return endIndex
        }
    }

    var destroyed = false
        private set

    override fun destroy() {
        if (!destroyed) {
            _entropy.fill(0)
            _words.fill(0.toChar())
        }
        destroyed = true
    }

    override fun close() = destroy()

    protected fun finalize() = destroy()

    override fun equals(other: Any?): Boolean =
        this === other || (other is MnemonicCode && MessageDigest.isEqual(entropy, other.entropy))

    override fun hashCode(): Int = entropy.contentHashCode()

    private inline fun <T> checkNotDestroyed(block: () -> T): T {
        check(!destroyed) { "MnemonicCode has already been destroyed" }
        return block()
    }
}
