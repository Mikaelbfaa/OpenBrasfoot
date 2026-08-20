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

    @Test
    fun `one minute alternates possession whatever happened in it`() {
        val clock = MatchClock(firstHalfMinutes = 46, secondHalfMinutes = 48)
        val before = initialState(setup(), TeamSide.HOME)
        val after = playMinute(before, minute = 40, clock = clock, rng = SplitMix64Rng(5L))

        assertEquals(TeamSide.AWAY, after.possessor, "possession alternates unconditionally")
        assertEquals(2, after.log.size - before.log.size, "a duel and one other event")
        assertTrue(after.log.all { it.minute == 40 }, "every event carries the minute it happened in")
    }
}
