package im.molly.monero

data class HttpRequest(
    val method: String?,
    val path: String?,
    val header: String?,
    val bodyBytes: ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HttpRequest

        if (method != other.method) return false
        if (path != other.path) return false
        if (header != other.header) return false
        if (bodyBytes != null) {
            if (other.bodyBytes == null) return false
            if (!bodyBytes.contentEquals(other.bodyBytes)) return false
        } else if (other.bodyBytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = method?.hashCode() ?: 0
        result = 31 * result + (path?.hashCode() ?: 0)
        result = 31 * result + (header?.hashCode() ?: 0)
        result = 31 * result + (bodyBytes?.contentHashCode() ?: 0)
        return result
    }
}
