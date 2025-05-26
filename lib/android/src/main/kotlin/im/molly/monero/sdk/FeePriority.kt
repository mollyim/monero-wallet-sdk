package im.molly.monero.sdk

enum class FeePriority(val priority: Int) {
    Low(1),
    Medium(2),
    High(3),
    Urgent(4),
}
