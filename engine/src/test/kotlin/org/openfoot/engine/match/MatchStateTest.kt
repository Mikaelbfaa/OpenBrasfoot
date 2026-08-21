package org.openfoot.engine.match

import org.openfoot.model.PlayerId
import org.openfoot.model.RuleSets
import org.openfoot.model.SplitMix64Rng
import org.openfoot.model.TeamSide
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MatchStateTest {

    private fun setup() = MatchSetup(
        home = Lineups.sideOfSlots(Lineups.FORMATION_4_4_2, 50, context = Lineups.context(isHome = true)),
        away = Lineups.sideOfSlots(Lineups.FORMATION_4_4_2, 50, context = Lineups.context(isHome = false)),
        season = 1,
        rules = RuleSets.CLASSIC,
    )

    @Test
    fun `a fresh state starts everyone on full energy`() {
        val state = initialState(setup(), TeamSide.HOME)
        val keeper = setup().home.lineup.first().id
        assertEquals(SideState.FULL_ENERGY, state.home.energy[keeper], "keeper energy")
    }

    @Test
    fun `a fresh state has nobody booked and nobody substituted`() {
        val state = initialState(setup(), TeamSide.HOME)
        assertTrue(state.home.bookings.isEmpty(), "bookings")
        assertEquals(0, state.home.substitutionsUsed, "substitutions used")
    }

    @Test
    fun `replacing one side leaves the other alone`() {
        val state = initialState(setup(), TeamSide.HOME)
        val changed = state.with(TeamSide.AWAY, state.away.copy(substitutionsUsed = 3))

        assertEquals(3, changed.of(TeamSide.AWAY).substitutionsUsed, "away")
        assertEquals(0, changed.of(TeamSide.HOME).substitutionsUsed, "home")
    }

    @Test
    fun `a squad with two players sharing an identity is refused`() {
        val twins = listOf(
            Lineups.player(slot = 1, strength = 50, id = 7),
            Lineups.player(slot = 2, strength = 50, id = 7),
        )
        val clash = MatchSetup(
            home = Lineups.side(twins),
            away = Lineups.sideOfSlots(Lineups.FORMATION_4_4_2, 50),
            season = 1,
            rules = RuleSets.CLASSIC,
        )

        val failure = assertFailsWith<IllegalArgumentException> {
            initialState(clash, TeamSide.HOME)
        }
        assertTrue(
            failure.message.orEmpty().contains("PlayerId(7)"),
            "the message should name the identity that collided, was: ${failure.message}",
        )
    }

    /**
     * A fresh state has nothing on record for section 3.8 either: no cards, no
     * injuries and no minutes planned. A side that has not been handed a plan
     * gets the empty one rather than a plan of nobody's choosing, which is
     * what lets a state be built by hand for a test of something else without
     * that test having to know anything about substitutions.
     */
    @Test
    fun `a fresh state carries no discipline record and no plan`() {
        val state = initialState(setup(), TeamSide.HOME)

        assertEquals(DisciplineCounts(), state.counts, "counts")
        assertEquals(SubstitutionPlan.NONE, state.home.plan, "home plan")
        assertEquals(SubstitutionPlan.NONE, state.away.plan, "away plan")
    }

    /**
     * A minute is the energy drain, then section 3.8's roll, then the tick, so
     * a minute can now log more than the duel and the tick's own event. This
     * seed's minute produces neither a card nor an injury, which the assertion
     * on the log size below says outright: two events, and both of them from
     * the tick. Playing a minute whose chain does fire is DisciplineChainTest's
     * job, not this one's.
     */
    @Test
    fun `one minute alternates possession whatever happened in it`() {
        val clock = MatchClock(firstHalfMinutes = 46, secondHalfMinutes = 48)
        val before = initialState(setup(), TeamSide.HOME)
        val after = playMinute(before, minute = 40, clock = clock, rng = SplitMix64Rng(5L))

        assertEquals(TeamSide.AWAY, after.possessor, "possession alternates unconditionally")
        assertEquals(2, after.log.size - before.log.size, "a duel and one other event")
        assertEquals(DisciplineCounts(), after.counts, "this seed's minute produced nothing")
        assertTrue(after.log.all { it.minute == 40 }, "every event carries the minute it happened in")
    }
}
