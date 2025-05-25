package im.molly.monero

/**
 * Shorthand aliases for [MoneroNetwork] enum values.
 *
 * These aliases allow direct usage of `Mainnet`, `Testnet`, and `Stagenet`
 * without qualifying them with `MoneroNetwork`.
 *
 * Example usage:
 * ```
 * import im.molly.monero.Mainnet
 *
 * val network = Mainnet
 * ```
 */

val Mainnet = MoneroNetwork.Mainnet
val Testnet = MoneroNetwork.Testnet
val Stagenet = MoneroNetwork.Stagenet
