package org.openfoot.engine.match

import org.openfoot.engine.world.ScriptedInts
import org.openfoot.model.Half
import org.openfoot.model.SplitMix64Rng
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The clock decides how long a match is. What matters is the draw order, since
 * it fixes the stream position of everything the match does afterwards, and the
 * range, since section 3.1 states it outright.
 *
 * randRange(from, to) is from + nextInt(to - from + 1), so a scripted integer is
 * the zero based draw, not the value the formula ends up using. extra1 =
 * randRange(0, 2) calls nextInt(3), so a scripted draw of 2 yields extra1 = 2.
 * extra2 = randRange(1, 5) calls nextInt(5), so a scripted draw of 0 yields
 * extra2 = 1.
 */
class MatchClockTest {

    /**
     * extra1 = randRange(0, 2) draws 2 -> 0 + 2 = 2 -> firstHalf = 45 + 2 = 47.
     * extra2 = randRange(1, 5) draws 0 -> 1 + 0 = 1 -> secondHalf = 45 + 1 = 46.
     */
    @Test
    fun `the first half takes the first draw and the second half the second`() {
        val clock = matchClock(ScriptedInts(2, 0))
        assertEquals(47, clock.firstHalfMinutes)
        assertEquals(46, clock.secondHalfMinutes)
    }

    @Test
    fun `the clock costs exactly two draws`() {
        val rng = ScriptedInts(0, 0)
        matchClock(rng)
        assertEquals(2, rng.draws)
    }

    /**
     * extra1 draws 0 -> 0 + 0 = 0. extra2 draws 0 -> 1 + 0 = 1.
     * total = 45 + 0 + 45 + 1 = 91, the minimum both ranges allow.
     */
    @Test
    fun `the shortest match the draws allow is ninety one minutes`() {
        assertEquals(91, matchClock(ScriptedInts(0, 0)).totalMinutes)
    }

    /**
     * extra1 draws 2 (its top bound, nextInt(3)) -> 0 + 2 = 2.
     * extra2 draws 4 (its top bound, nextInt(5)) -> 1 + 4 = 5.
     * total = 45 + 2 + 45 + 5 = 97, the maximum both ranges allow.
     */
    @Test
    fun `the longest match the draws allow is ninety seven minutes`() {
        assertEquals(97, matchClock(ScriptedInts(2, 4)).totalMinutes)
    }

    /**
     * extra1 in 0..2 and extra2 in 1..5, so total = 90 + extra1 + extra2
     * ranges over 91..97 regardless of which generator draws it.
     */
    @Test
    fun `every clock a real generator produces stays inside the stated range`() {
        repeat(2000) { seed ->
            val total = matchClock(SplitMix64Rng(seed.toLong())).totalMinutes
            assertTrue(total in 91..97, "seed $seed gave $total minutes")
        }
    }

    @Test
    fun `the half boundary falls where the first half ends`() {
        val clock = MatchClock(firstHalfMinutes = 46, secondHalfMinutes = 48)
        assertEquals(Half.FIRST, clock.halfOf(0))
        assertEquals(Half.FIRST, clock.halfOf(45))
        assertEquals(Half.SECOND, clock.halfOf(46))
        assertEquals(Half.SECOND, clock.halfOf(93))
    }

    /**
     * Minutes into the half restart at the interval, which is what the energy
     * drain of section 3.9 and the discipline phase of section 3.8 both count.
     */
    @Test
    fun `minutes into the half restart at the interval`() {
        val clock = MatchClock(firstHalfMinutes = 46, secondHalfMinutes = 49)
        assertEquals(0, clock.intoHalf(0))
        assertEquals(45, clock.intoHalf(45))
        assertEquals(0, clock.intoHalf(46))
        assertEquals(48, clock.intoHalf(94))
    }
}
