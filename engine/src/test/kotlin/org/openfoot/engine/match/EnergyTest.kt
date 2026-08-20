package org.openfoot.engine.match

import org.openfoot.model.RuleSets
import org.openfoot.model.TeamSide
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Section 3.9's brackets are inclusive upper bounds, so a player exactly on a
 * boundary pays the cheaper cost. The cases below sit on both sides of every
 * boundary, because an off by one here would be invisible in aggregate and
 * would quietly change how often the AI substitutes once section 3.8 lands.
 */
class EnergyTest {

    private val rules = RuleSets.CLASSIC

    private fun setup() = MatchSetup(
        home = Lineups.sideOfSlots(Lineups.FORMATION_4_4_2, 50, context = Lineups.context(isHome = true)),
        away = Lineups.sideOfSlots(Lineups.FORMATION_4_4_2, 50, context = Lineups.context(isHome = false)),
        season = 1,
        rules = rules,
    )

    @Test
    fun `the cost brackets are inclusive upper bounds`() {
        assertEquals(1, energyCost(20, rules), "twenty is in the first bracket")
        assertEquals(2, energyCost(21, rules), "twenty one is in the second")
        assertEquals(2, energyCost(25, rules), "twenty five is in the second")
        assertEquals(3, energyCost(26, rules), "twenty six is in the third")
        assertEquals(3, energyCost(31, rules), "thirty one is in the third")
        assertEquals(4, energyCost(32, rules), "thirty two is in the fourth")
        assertEquals(4, energyCost(36, rules), "thirty six is in the fourth")
        assertEquals(5, energyCost(37, rules), "thirty seven falls off the end")
    }

    @Test
    fun `a whole match drains a twenty four year old by about twenty eight`() {
        val clock = MatchClock(firstHalfMinutes = 46, secondHalfMinutes = 48)
        val drains = (0 until clock.totalMinutes).count { drainsThisMinute(it, clock, rules) }

        assertEquals(14, drains, "seven per half, which is what the 28 in section 3.9 implies")
        assertEquals(28, drains * energyCost(24, rules), "the figure section 3.9 quotes")
    }

    @Test
    fun `the drain restarts at the top of the second half`() {
        val clock = MatchClock(firstHalfMinutes = 46, secondHalfMinutes = 48)

        assertTrue(drainsThisMinute(0, clock, rules), "the first minute of the first half")
        assertTrue(drainsThisMinute(46, clock, rules), "the first minute of the second half")
        assertFalse(drainsThisMinute(45, clock, rules), "still the first half, not on the interval")
    }

    /**
     * The second call uses minute 46, not the 49 a first draft of this test used.
     * With firstHalfMinutes = 46, the second half's own drains restart at offset
     * nought within it (see the restart test below), so its scheduled minutes are
     * 46, 53, 60, ... Minute 49 is offset 3 into the second half, 3 % 7 != 0, so
     * it is not a drain minute at all and the keeper would wrongly appear still
     * exempt for lack of any drain happening, not because of the exemption. 46 is
     * the second half's first scheduled drain, so it isolates the exemption.
     */
    @Test
    fun `the keeper is exempt in the first half only`() {
        val state = initialState(setup(), TeamSide.HOME)
        val clock = MatchClock(firstHalfMinutes = 46, secondHalfMinutes = 48)
        val keeper = state.setup.home.lineup.first { it.slot.value == 1 }.id

        val firstHalf = state.drainEnergy(minute = 7, clock = clock)
        assertEquals(SideState.FULL_ENERGY, firstHalf.home.energy[keeper], "exempt before the break")

        val secondHalf = firstHalf.drainEnergy(minute = 46, clock = clock)
        assertTrue(secondHalf.home.energy.getValue(keeper) < SideState.FULL_ENERGY, "not after it")
    }

    @Test
    fun `a substitute on the bench is not drained`() {
        val substitute = Lineups.player(slot = -1, strength = 50, id = 99, age = 37)
        val state = initialState(
            setup = setup(),
            startingPossessor = TeamSide.HOME,
            homeBench = listOf(substitute),
        )
        val clock = MatchClock(firstHalfMinutes = 46, secondHalfMinutes = 48)

        val drained = state.drainEnergy(minute = 7, clock = clock)

        assertEquals(
            SideState.FULL_ENERGY,
            drained.home.energy[substitute.id],
            "section 3.9 drains players in the match, and a substitute comes on with what he has",
        )
    }
}
