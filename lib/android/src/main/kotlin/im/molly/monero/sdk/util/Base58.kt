package im.molly.monero.sdk.util

import java.nio.ByteBuffer
import java.nio.charset.Charset

const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

object Decoder {
    private val decodingTable = IntArray(128) { ALPHABET.indexOf(it.toChar()) }

    private val blockSizes = listOf(0, -1, 1, 2, -1, 3, 4, 5, -1, 6, 7, 8)

    fun decode(input: ByteArray): ByteBuffer {
        val size = input.size
        val needed = 8 * (size / 11) + findOutputBlockSize(size % 11)
        val out = ByteBuffer.allocate(needed)
        var pos = 0
        while (pos + 11 <= size) {
            decodeBlock(input, pos, 11, out)
            pos += 11
        }
        val remain = size - pos
        if (remain > 0) {
            decodeBlock(input, pos, remain, out)
        }
        out.flip()
        return out
    }

    private fun decodeBlock(block: ByteArray, offset: Int, len: Int, out: ByteBuffer) {
        val blockSize = findOutputBlockSize(len)
        val newOutPos = out.position() + blockSize

        var num = 0uL
        var base = 1uL
        var zeroes = 0

        for (i in (offset + len - 1) downTo offset) {
            val c = block[i].toInt()
            val digit = decodingTable.getOrElse(c) { -1 }.toULong()
            require(digit >= 0uL) { "Invalid symbol" }
            if (digit == 0uL) {
                zeroes++
            } else {
                while (zeroes > 0) {
                    base *= 58u
                    zeroes--
                }
                val prod = digit * base
                val lastNum = num
                num += prod
                require((prod / base == digit) && (num > lastNum)) { "Overflow" }
                base *= 58u  // Never overflows, 58^10 < 2^64
            }
        }
        for (j in 1..blockSize) {
            out.put(newOutPos - j, num.toByte())
            num = num shr 8
        }
        require(num == 0uL) { "Overflow" }
        out.position(newOutPos)
    }

    private fun findOutputBlockSize(blockSize: Int): Int =
        blockSizes[blockSize].also {
            require(it >= 0) { "Invalid block size" }
        }
}

fun String.decodeBase58(): ByteArray =
    Decoder.decode(this.toByteArray(Charset.defaultCharset())).array()
