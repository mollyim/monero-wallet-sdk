package im.molly.monero.exceptions

class InternalRuntimeException(message: String, cause: Throwable? = null) : RuntimeException(
    buildString {
        append(message.trimEnd('.'))
        append(". This is likely a bug; please report the issue to the Monero SDK team on GitHub.")
    },
    cause
)
