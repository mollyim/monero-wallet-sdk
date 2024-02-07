package im.molly.monero

enum class FeePriority(val priority: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    URGENT(4),
}
