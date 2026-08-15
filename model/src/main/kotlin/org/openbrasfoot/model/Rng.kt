package org.openbrasfoot.model

/**
 * Source of randomness for the simulation.
 *
 * The original game allocates a fresh unseeded generator per draw and is not
 * reproducible. This project is fully deterministic instead: a career replays
 * exactly from its seed, which is what makes bug reports actionable.
 *
 * Implementations must never delegate to the platform generators, whose output
 * is not guaranteed stable across JDK versions.
 */
interface Rng {

    /** Uniformly distributed 64 bits. */
    fun nextBits(): Long

    /** Uniform value in the half open range zero until bound. */
    fun nextInt(bound: Int): Int

    /** Uniform value in the half open range zero until one. */
    fun nextDouble(): Double

    /**
     * Derives an independent child stream from this generator's origin seed.
     *
     * The child depends only on the origin and the tag, never on how many
     * values the parent has already produced. That is what makes simulation
     * order irrelevant, so a round of matches gives the same results whether it
     * runs sequentially or across threads.
     */
    fun fork(tag: Long): Rng
}

/**
 * SplitMix64. Small, fast, and specified precisely enough that the sequence is
 * stable forever on every platform.
 */
class SplitMix64Rng(private val origin: Long) : Rng {

    private var state: Long = origin

    override fun nextBits(): Long {
        state += GOLDEN_GAMMA
        return mix64(state)
    }

    override fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive, was $bound" }
        while (true) {
            val bits = (nextBits() ushr 33).toInt()
            val value = bits % bound
            if (bits - value + (bound - 1) >= 0) {
                return value
            }
        }
    }

    override fun nextDouble(): Double = (nextBits() ushr 11) * DOUBLE_UNIT

    override fun fork(tag: Long): Rng = SplitMix64Rng(deriveSeed(origin, tag))

    private companion object {
        const val GOLDEN_GAMMA = -0x61c8864680b583ebL
        const val DOUBLE_UNIT = 1.0 / (1L shl 53)
    }
}

/**
 * The SplitMix64 finalizer. Turns a counter into well distributed bits.
 */
internal fun mix64(value: Long): Long {
    var z = value
    z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
    z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
    return z xor (z ushr 31)
}

/**
 * Combines a root seed with positional tags into a child seed.
 *
 * Seeds are derived from position in the world, for example season then
 * competition then round then the two club ids, so any single match can be
 * replayed on its own without simulating everything before it.
 */
fun deriveSeed(root: Long, vararg tags: Long): Long {
    var acc = root
    for (tag in tags) {
        acc = mix64(acc xor (tag * FNV_PRIME))
    }
    return acc
}

private const val FNV_PRIME = 0x100000001b3L

/**
 * Domain separators for seed derivation. Distinct values keep unrelated parts
 * of the simulation from ever sharing a stream.
 */
object SeedDomain {
    const val WORLDGEN = 1L
    const val SEASON = 2L
    const val FIXTURES = 3L
    const val MATCH = 4L
    const val WEEK = 5L
    const val EVOLUTION = 6L
    const val MARKET = 7L
    const val END_SEASON = 8L
    const val USER_ACTION = 9L
}

/**
 * Mirrors the spec idiom rand(N), an integer from zero to N minus one.
 */
fun Rng.rand(bound: Int): Int = nextInt(bound)

/**
 * Mirrors the spec idiom rand(a..b) with both ends included.
 */
fun Rng.randRange(fromInclusive: Int, toInclusive: Int): Int {
    require(toInclusive >= fromInclusive) {
        "empty range $fromInclusive..$toInclusive"
    }
    return fromInclusive + nextInt(toInclusive - fromInclusive + 1)
}
