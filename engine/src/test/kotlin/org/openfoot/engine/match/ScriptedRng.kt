package org.openfoot.engine.match

import org.openfoot.model.Rng

/**
 * A generator that hands out a fixed sequence of doubles.
 *
 * Lets a test pin a documented probability exactly, by feeding the draw just
 * below and just above a boundary, with no sampling error at all. The other
 * methods throw so that a formula quietly reaching for different randomness
 * shows up as a failure rather than as noise.
 */
class ScriptedRng(private vararg val doubles: Double) : Rng {

    var draws: Int = 0
        private set

    override fun nextDouble(): Double {
        check(draws < doubles.size) {
            "ScriptedRng ran out after $draws draws"
        }
        return doubles[draws++]
    }

    override fun nextBits(): Long = throw UnsupportedOperationException("nextBits is not scripted")

    override fun nextInt(bound: Int): Int =
        throw UnsupportedOperationException("nextInt is not scripted")

    override fun fork(tag: Long): Rng = throw UnsupportedOperationException("fork is not scripted")
}
