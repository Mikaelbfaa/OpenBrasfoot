package org.openfoot.engine.match

import org.openfoot.engine.world.ScriptedInts
import org.openfoot.model.RuleSets
import org.openfoot.model.SplitMix64Rng
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The minutes a side plans to make a change in, drawn once per match per side.
 *
 * Every minute here is counted inside the second half, which is the only half
 * section 3.8 lets the AI substitute in.
 *
 * The scripted tests below all script one whole plan, in the fixed draw order
 * substitutionPlan consumes:
 *
 * 1. the first chasing minute, as an offset into nineteen to thirty eight
 * 2. the second chasing minute, same offset, redrawn when it repeats
 * 3. the third chasing minute's coin, out of a hundred
 * 4. the third chasing minute, only when the coin came in under sixty nine
 * 5. the routine pool selector, out of a hundred
 * 6. the first routine minute, as an offset into the chosen pool
 * 7. the second routine minute, same offset, redrawn when it repeats
 * 8. the first late coin, out of a hundred
 * 9. its minute, only when that coin came in under seventy nine, as an offset
 *    into forty three to forty seven
 * 10. the second late coin, out of a hundred
 * 11. its minute, only when that coin came in under forty nine
 * 12. the interval coin, out of a hundred
 *
 * ScriptedInts throws when a plan asks for a draw the script does not have and
 * counts the ones it made, so a script that is exactly the right length is
 * itself the assertion that the order above is the order the code uses.
 */
class SubstitutionPlanTest {

    /**
     * The third chasing minute arrives on a coin under sixty nine. Sixty eight
     * buys one and sixty nine does not, which pins the boundary from both
     * sides; the sixty nine per cent section 3.8 prints is exactly the sixty
     * nine draws of a hundred below it.
     */
    @Test
    fun `a coin under sixty nine buys a third chasing minute`() {
        assertEquals(
            listOf(19, 20),
            plan(0, 1, 69, POOL_LATE, 0, 6, *NO_TAIL).chasing,
        )
        assertEquals(
            listOf(19, 20, 21),
            plan(0, 1, 68, 2, POOL_LATE, 0, 6, *NO_TAIL).chasing,
        )
    }

    /**
     * The chasing window is nineteen to thirty eight inclusive: offset nought
     * is nineteen and offset nineteen is thirty eight, so both ends of the
     * window are reachable and neither runs past it.
     */
    @Test
    fun `the chasing window runs from nineteen to thirty eight`() {
        assertEquals(
            listOf(19, 38),
            plan(0, 19, 69, POOL_LATE, 0, 6, *NO_TAIL).chasing,
        )
    }

    /**
     * The two chasing minutes are drawn without replacement, and a repeat
     * costs one further draw rather than being quietly dropped. The second
     * draw repeats the first and the third draw, offset five, is the one that
     * lands.
     */
    @Test
    fun `a repeated chasing minute is drawn again`() {
        assertEquals(
            listOf(19, 24),
            plan(0, 0, 5, 69, POOL_LATE, 0, 6, *NO_TAIL).chasing,
        )
    }

    /**
     * The routine pool is chosen by one draw out of a hundred, and all three
     * of section 3.8's bands are pinned on both of their boundaries: above
     * ninety takes five to fifteen, above fifty takes sixteen to thirty five,
     * and anything else takes thirty six to forty two.
     *
     * The two offsets in each case are nought and the last index of that pool,
     * so both ends of all three windows are pinned as well. Nine, forty and
     * fifty one draws of a hundred are the nine, forty and fifty one per cent
     * the spec prints beside them.
     */
    @Test
    fun `the routine pool table maps every band to its window`() {
        assertEquals(listOf(36, 42), routineOf(selector = 0, offsets = intArrayOf(0, 6)))
        assertEquals(listOf(36, 42), routineOf(selector = 50, offsets = intArrayOf(0, 6)))
        assertEquals(listOf(16, 35), routineOf(selector = 51, offsets = intArrayOf(0, 19)))
        assertEquals(listOf(16, 35), routineOf(selector = 90, offsets = intArrayOf(0, 19)))
        assertEquals(listOf(5, 15), routineOf(selector = 91, offsets = intArrayOf(0, 10)))
        assertEquals(listOf(5, 15), routineOf(selector = 99, offsets = intArrayOf(0, 10)))
    }

    /**
     * The two routine minutes come from one pool without replacement, the same
     * way the chasing ones do. The second draw repeats the first and the
     * third, offset three, is the one that lands.
     */
    @Test
    fun `a repeated routine minute is drawn again`() {
        assertEquals(
            listOf(36, 39),
            plan(0, 1, 69, POOL_LATE, 0, 0, 3, *NO_TAIL).routine,
        )
    }

    /**
     * Two more routine minutes can follow from forty three to forty seven, one
     * on a coin under seventy nine and one on a coin under forty nine. Seventy
     * eight and forty eight buy them; seventy nine and forty nine, in the
     * NO_TAIL script every other test uses, do not.
     *
     * The offsets four and nought put the two at forty seven and forty three,
     * pinning both ends of that window, and they stay in draw order rather
     * than being sorted.
     */
    @Test
    fun `the two late coins each buy a minute from forty three to forty seven`() {
        assertEquals(
            listOf(36, 37, 47, 43),
            plan(0, 1, 69, POOL_LATE, 0, 1, 78, 4, 48, 0, 49).routine,
        )
        assertEquals(
            listOf(36, 37),
            plan(0, 1, 69, POOL_LATE, 0, 1, *NO_TAIL).routine,
        )
    }

    /**
     * The second late minute may not repeat the first, and a repeat costs one
     * further draw. Offset nought lands on forty three twice and offset two,
     * drawn again, lands on forty five.
     */
    @Test
    fun `a repeated late minute is drawn again`() {
        assertEquals(
            listOf(36, 37, 43, 45),
            plan(0, 1, 69, POOL_LATE, 0, 1, 78, 0, 48, 0, 2, 49).routine,
        )
    }

    /**
     * The interval's coin is the last draw of the plan and swaps under fifty,
     * so forty nine swaps and fifty does not.
     */
    @Test
    fun `the interval coin swaps under fifty`() {
        assertTrue(plan(0, 1, 69, POOL_LATE, 0, 6, 79, 49, 49).halfTimeSwap)
        assertFalse(plan(0, 1, 69, POOL_LATE, 0, 6, 79, 49, 50).halfTimeSwap)
    }

    /**
     * The order above is the whole order: a plan that takes neither the third
     * chasing minute nor either late one makes exactly nine draws, and one
     * that takes all three makes exactly twelve. ScriptedInts throws on a
     * tenth or a thirteenth, so this pins the count from both directions.
     */
    @Test
    fun `a plan makes exactly the draws the order names`() {
        val lean = ScriptedInts(0, 1, 69, POOL_LATE, 0, 6, 79, 49, 50)
        substitutionPlan(lean, RULES)
        assertEquals(9, lean.draws)

        val full = ScriptedInts(0, 1, 68, 2, POOL_LATE, 0, 1, 78, 0, 48, 1, 49)
        substitutionPlan(full, RULES)
        assertEquals(12, full.draws)
    }

    @Test
    fun `chasing minutes fall inside the window and never repeat`() {
        for (seed in 1L..200L) {
            val plan = substitutionPlan(SplitMix64Rng(seed), RULES)
            assertTrue(plan.chasing.all { it in 19..38 }, "seed $seed")
            assertEquals(plan.chasing.size, plan.chasing.toSet().size, "seed $seed")
            assertTrue(plan.chasing.size in 2..3, "seed $seed")
        }
    }

    @Test
    fun `routine minutes come from one pool and never repeat`() {
        for (seed in 1L..200L) {
            val plan = substitutionPlan(SplitMix64Rng(seed), RULES)
            val fromPool = plan.routine.take(2)
            assertTrue(
                fromPool.all { it in 5..15 } ||
                    fromPool.all { it in 16..35 } ||
                    fromPool.all { it in 36..42 },
                "seed $seed drew $fromPool from more than one pool",
            )
            assertTrue(plan.routine.drop(2).all { it in 43..47 }, "seed $seed")
            assertTrue(plan.routine.size in 2..4, "seed $seed")
            assertEquals(plan.routine.size, plan.routine.toSet().size, "seed $seed")
        }
    }

    /**
     * The fifty one per cent band is the majority one, and the nine per cent
     * band is the rare one, which is the shape of the table read back out of
     * two thousand plans rather than off its boundaries.
     */
    @Test
    fun `the late pool is the most common and the early pool the rarest`() {
        val plans = (1L..2000L).map { substitutionPlan(SplitMix64Rng(it), RULES) }
        val late = plans.count { it.routine.first() in 36..42 }
        val early = plans.count { it.routine.first() in 5..15 }
        assertTrue(late > plans.size / 2, "the fifty one per cent pool came up $late times")
        assertTrue(early < plans.size / 5, "the nine per cent pool came up $early times")
    }

    /**
     * The interval coin comes up heads about half the time over five hundred
     * plans, which is the fifty per cent of section 3.8 read back out of the
     * stream rather than off its boundary.
     */
    @Test
    fun `the interval coin is close to even over many plans`() {
        val plans = (1L..500L).map { substitutionPlan(SplitMix64Rng(it), RULES) }
        val heads = plans.count { it.halfTimeSwap }
        assertTrue(heads in 200..300, "the fifty per cent coin came up $heads times in 500")
    }

    private companion object {
        val RULES = RuleSets.CLASSIC

        /**
         * A pool selector draw that lands in the thirty six to forty two band,
         * used by every test that is about something other than the pool
         * table itself.
         */
        const val POOL_LATE = 0

        /**
         * The three draws that close a plan without buying anything: the two
         * late coins at their first refusing value and the interval coin at
         * its own, seventy nine, forty nine and fifty.
         */
        val NO_TAIL = intArrayOf(79, 49, 50)

        fun plan(vararg draws: Int): SubstitutionPlan =
            substitutionPlan(ScriptedInts(*draws), RULES)

        /**
         * The routine minutes of a plan whose pool selector and two pool
         * offsets are given and whose every other draw refuses.
         */
        fun routineOf(selector: Int, offsets: IntArray): List<Int> =
            plan(0, 1, 69, selector, offsets[0], offsets[1], *NO_TAIL).routine
    }
}
