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

    /**
     * Item 31 derives seven drains a half from a regulation half of forty five
     * minutes counted from nought: 0, 7, 14, 21, 28, 35 and 42. The code
     * counts over the real half instead, and section 3.1 draws second half
     * stoppage from one to five, so the longest second half runs fifty minutes
     * and a drain lands on minute 49 into it as well. That is an eighth drain,
     * and it is the code's behaviour rather than a defect of the derivation:
     * the derivation is about regulation time and the match is not.
     *
     * The first half cannot do the same. Its stoppage is nought to two, so it
     * runs at most forty seven minutes and offset 49 never arrives.
     *
     * The clock here is built from the constants of section 3.1 rather than
     * from literals, so this reads as the longest legal match and stays that
     * way if the stoppage spreads are ever corrected. The counts are the
     * assertion and were worked out by hand.
     */
    @Test
    fun `the longest legal second half drains an eighth time`() {
        val longest = MatchClock(
            firstHalfMinutes = REGULATION_HALF_MINUTES + FIRST_HALF_STOPPAGE_MAX,
            secondHalfMinutes = REGULATION_HALF_MINUTES + SECOND_HALF_STOPPAGE_MAX,
        )

        val firstHalf = (0 until longest.firstHalfMinutes)
            .count { drainsThisMinute(it, longest, rules) }
        val secondHalf = (longest.firstHalfMinutes until longest.totalMinutes)
            .count { drainsThisMinute(it, longest, rules) }

        assertEquals(7, firstHalf, "a forty seven minute half still stops one drain short of eight")
        assertEquals(8, secondHalf, "a fifty minute half reaches offset 49, which is a drain minute")
        assertEquals(
            30,
            (firstHalf + secondHalf) * energyCost(24, rules),
            "so the longest match costs a twenty four year old thirty, not the twenty eight " +
                "section 3.9 quotes for a whole match",
        )
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
