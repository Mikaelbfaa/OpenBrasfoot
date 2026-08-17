package org.openfoot.engine.world

import org.openfoot.model.Rng

/**
 * A generator that hands out a fixed sequence of integers.
 *
 * World generation draws only through the integer idiom of the spec, so a test
 * can state every draw a formula is entitled to make and then read the result
 * off the spec by hand. The other methods throw, which turns a formula reaching
 * for randomness it should not into a failure rather than into noise.
 *
 * The sibling of this in the match tests scripts doubles instead. They stay
 * separate because no test needs both, and merging them would make the draw
 * counter ambiguous.
 */
class ScriptedInts(private vararg val values: Int) : Rng {

    var draws: Int = 0
        private set

    override fun nextInt(bound: Int): Int {
        check(draws < values.size) {
            "ScriptedInts ran out after $draws draws"
        }
        val value = values[draws++]
        check(value in 0 until bound) {
            "scripted draw $value is outside the bound $bound the formula asked for"
        }
        return value
    }

    override fun nextDouble(): Double =
        throw UnsupportedOperationException("nextDouble is not scripted")

    override fun nextBits(): Long = throw UnsupportedOperationException("nextBits is not scripted")

    override fun fork(tag: Long): Rng = throw UnsupportedOperationException("fork is not scripted")
}
