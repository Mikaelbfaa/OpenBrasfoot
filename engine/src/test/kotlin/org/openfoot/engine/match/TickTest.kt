package org.openfoot.engine.match

import org.openfoot.model.RuleSets
import org.openfoot.model.TeamSide
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The tick is scripted rather than sampled, so each path is pinned by the draws
 * it is entitled to make.
 *
 * The two sides are built at equal strength from Lineups, whose default
 * competition kind is a friendly, which is not neutral ground under section
 * 3.11. Home advantage therefore applies on the home side's ticks, and every
 * boundary below is worked out by hand from section 3.6 with that in mind.
 */
class TickTest {

    private fun evenSetup(season: Int = 1): MatchSetup {
        val home = Lineups.sideOfSlots(
            Lineups.FORMATION_4_4_2,
            strength = 50,
            context = Lineups.context(isHome = true),
        )
        val away = Lineups.sideOfSlots(
            Lineups.FORMATION_4_4_2,
            strength = 50,
            context = Lineups.context(isHome = false),
        )
        return MatchSetup(home, away, season = season, rules = RuleSets.CLASSIC)
    }

    @Test
    fun `a lost possession duel costs two draws and yields a loose ball`() {
        val rng = ScriptedRng(0.99, 0.10)
        val outcome = playTick(evenSetup(), TeamSide.HOME, goalsScoredByPossessor = 0, rng = rng)

        assertEquals(TeamSide.AWAY, outcome.possessionWinner)
        assertEquals(TeamSide.HOME, outcome.possessor)
        assertTrue(!outcome.isShot)
        assertEquals(2, rng.draws)
    }

    @Test
    fun `a tackle is credited to the side that was not in possession`() {
        val rng = ScriptedRng(0.99, 0.10)
        val outcome = playTick(evenSetup(), TeamSide.HOME, 0, rng)
        assertEquals(TickEvent.TACKLE, outcome.event)
    }

    @Test
    fun `the other half of the coin is a misplaced pass`() {
        val rng = ScriptedRng(0.99, 0.90)
        val outcome = playTick(evenSetup(), TeamSide.HOME, 0, rng)
        assertEquals(TickEvent.MISPLACED_PASS, outcome.event)
    }

    /**
     * Finding 3. The two tests above only pin that 0.10 lands on TACKLE and
     * 0.90 lands on MISPLACED_PASS, which is also true of a lopsided split, so
     * they cannot tell a 70/30 coin from the even one section 3.5 documents.
     * The coin is EVEN_COIN, weights [1.0, 1.0] with default multipliers of
     * [1.0, 1.0], so weightedPick's own contract fixes the boundary exactly:
     * total is 2.0, and index 0 (TACKLE) is picked while running (1.0) is
     * strictly greater than the draw (nextDouble times total), so the boundary
     * sits at a raw nextDouble of 0.5 and the comparison is strict. That is the
     * same weights and multipliers WeightedChoiceTest already exercises at
     * 0.49, 0.5 and 0.51, reused here through the real coin rather than the
     * primitive directly.
     */
    @Test
    fun `the loose ball coin is even right at one half, not some wider split`() {
        val justBelow = playTick(evenSetup(), TeamSide.HOME, 0, ScriptedRng(0.99, 0.49)).event
        val onTheBoundary = playTick(evenSetup(), TeamSide.HOME, 0, ScriptedRng(0.99, 0.5)).event
        val justAbove = playTick(evenSetup(), TeamSide.HOME, 0, ScriptedRng(0.99, 0.51)).event

        assertEquals(TickEvent.TACKLE, justBelow, "a draw just under 0.5 must still select TACKLE")
        assertEquals(
            TickEvent.MISPLACED_PASS,
            onTheBoundary,
            "weightedPick's strict comparison sends a draw exactly on the boundary to the " +
                "following outcome, MISPLACED_PASS",
        )
        assertEquals(TickEvent.MISPLACED_PASS, justAbove, "a draw just over 0.5 must select MISPLACED_PASS")
    }

    @Test
    fun `a won duel with no chance costs three draws`() {
        val rng = ScriptedRng(0.01, 0.99, 0.10)
        val outcome = playTick(evenSetup(), TeamSide.HOME, 0, rng)

        assertEquals(TeamSide.HOME, outcome.possessionWinner)
        assertTrue(!outcome.isShot)
        assertEquals(3, rng.draws)
    }

    @Test
    fun `a chance becomes a shot and costs four draws`() {
        val rng = ScriptedRng(0.01, 0.01, 0.50, 0.01)
        val outcome = playTick(evenSetup(), TeamSide.HOME, 0, rng)

        assertTrue(outcome.isShot)
        assertEquals(4, rng.draws)
    }

    @Test
    fun `every shot outcome maps onto a tick event`() {
        val events = listOf(0.01, 0.50, 0.99).map { shotDraw ->
            playTick(evenSetup(), TeamSide.HOME, 0, ScriptedRng(0.01, 0.01, 0.50, shotDraw)).event
        }
        assertEquals(3, events.toSet().size, "each shot outcome must map to its own event: $events")
        assertTrue(events.all { it != TickEvent.TACKLE && it != TickEvent.MISPLACED_PASS })
    }

    @Test
    fun `the possessor's own goals feed the anti blowout ladder`() {
        val fresh = playTick(evenSetup(), TeamSide.HOME, 0, ScriptedRng(0.01, 0.01, 0.50, 0.05))
        val leading = playTick(evenSetup(), TeamSide.HOME, 6, ScriptedRng(0.01, 0.01, 0.50, 0.05))

        assertEquals(TickEvent.GOAL, fresh.event)
        assertTrue(leading.event != TickEvent.GOAL, "six goals in must collapse the conversion rate")
    }

    @Test
    fun `the tick never alternates possession itself`() {
        val outcome = playTick(evenSetup(), TeamSide.AWAY, 0, ScriptedRng(0.99, 0.10))
        assertEquals(TeamSide.AWAY, outcome.possessor)
    }
}
