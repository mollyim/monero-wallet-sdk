package im.molly.monero.util

fun CharSequence.parseHex(): ByteArray {
    check(length % 2 == 0) { "Must have an even length" }

    return ByteArray(length / 2) {
        Integer.parseInt(substring(it * 2, (it + 1) * 2), 16).toByte()
    }
}

fun ByteArray.toHex(): String = joinToString(separator = "") { "%02x".format(it) }
