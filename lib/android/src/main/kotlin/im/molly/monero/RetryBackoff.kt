package im.molly.monero

import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

interface BackoffPolicy {
    /**
     * Returns the amount of time to wait before performing a retry or a reconnect, based on the current [retryCount].
     */
    fun waitTime(retryCount: Int): Duration
}

/**
 * A [BackoffPolicy] based on exponential backoff and jitter.
 *
 * @param minBackoff Set the minimum [Duration] for the first backoff.
 * @param maxBackoff Set a hard maximum [Duration] for exponential backoff.
 */
class ExponentialBackoff(
    private val minBackoff: Duration,
    private val maxBackoff: Duration,
    private val multiplier: Double,
    private val jitter: Double,
) : BackoffPolicy {
    init {
        require(minBackoff.isPositive())
        require(maxBackoff.isPositive())
        require(multiplier > 1.0)
        require(jitter < 1.0)
    }

    override fun waitTime(retryCount: Int): Duration =
        if (retryCount > 0) {
            addJitter((minBackoff * multiplier.pow(retryCount)).coerceAtMost(maxBackoff))
        } else {
            Duration.ZERO
        }

    private fun addJitter(waitTime: Duration): Duration {
        val jitterAmount = waitTime.inWholeMilliseconds * jitter
        val jitter = Random.nextDouble(-jitterAmount, jitterAmount)
        return waitTime + jitter.toDuration(DurationUnit.MILLISECONDS)
    }

    companion object {
        val Default = ExponentialBackoff(
            minBackoff = 1.seconds,
            maxBackoff = 20.seconds,
            multiplier = 1.6,
            jitter = 0.2,
        )
    }
}
