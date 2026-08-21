package org.openfoot.engine.match

import org.openfoot.engine.world.ScriptedInts
import org.openfoot.model.RuleSets
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Section 3.8's injury duration, the one place in the whole engine where
 * energy feeds back into an outcome.
 *
 * Every case below states its three draws in order: the short term x from
 * rand(0..13), the long term y offset from rand(0..19), and the severity from
 * rand(100). All are drawn before the age is consulted, so the stream costs
 * the same whatever the player's age, which is what keeps a squad's ages from
 * shifting every later draw of the match.
 */
class InjuryTest {

    /**
     * A twenty year old discards the energy term entirely, so a player of that
     * age on empty lasts exactly as long as one on full: x days and no more.
     * With x drawn as 6, y as 0 and no severity bonus, that is six days both
     * times.
     */
    @Test
    fun `the youngest players discard the energy term`() {
        val onEmpty = injuryOutcome(age = 20, energy = 0, rules = RULES, rng = ScriptedInts(6, 0, 50))
        val onFull = injuryOutcome(age = 20, energy = 100, rules = RULES, rng = ScriptedInts(6, 0, 50))
        assertEquals(6, onEmpty.days)
        assertEquals(6, onFull.days)
    }

    /**
     * Under ten energy adds a base of five, under fifty adds one, and anything
     * above adds nothing. A twenty five year old adds a further constant of
     * one. With x drawn as 6 and no severity bonus that is 5 + 6 + 1, then
     * 1 + 6 + 1, then 0 + 6 + 1.
     */
    @Test
    fun `energy sets the base three ways`() {
        assertEquals(12, injuryOutcome(25, 9, RULES, ScriptedInts(6, 0, 50)).days)
        assertEquals(8, injuryOutcome(25, 49, RULES, ScriptedInts(6, 0, 50)).days)
        assertEquals(7, injuryOutcome(25, 50, RULES, ScriptedInts(6, 0, 50)).days)
    }

    /**
     * The three age brackets that add a flat constant: 25 adds one, 30 adds
     * two, 35 adds three. Full energy, so the base is nought, and x is 6.
     */
    @Test
    fun `each age bracket adds its own constant`() {
        assertEquals(7, injuryOutcome(25, 100, RULES, ScriptedInts(6, 0, 50)).days)
        assertEquals(8, injuryOutcome(30, 100, RULES, ScriptedInts(6, 0, 50)).days)
        assertEquals(9, injuryOutcome(35, 100, RULES, ScriptedInts(6, 0, 50)).days)
    }

    /**
     * Twenty is the last age that discards the energy term outright; twenty
     * one is the first that takes the flat constant of one instead. Full
     * energy, so the base is nought, and x is 6: 0 + 6 + 1 is seven, not the
     * six a player one year younger would get.
     */
    @Test
    fun `the second age bracket starts at twenty one`() {
        assertEquals(7, injuryOutcome(21, 100, RULES, ScriptedInts(6, 0, 50)).days)
    }

    /**
     * From thirty six the long term y replaces the constant, and y is five
     * plus rand(0..19). With x as 6 and the y draw as 4 that is 6 + 9.
     */
    @Test
    fun `players past thirty five draw the long term`() {
        assertEquals(15, injuryOutcome(36, 100, RULES, ScriptedInts(6, 4, 50)).days)
    }

    /**
     * Forty five is the last age that draws the long term alone, with no
     * further constant added on top; forty six is the first to add the extra
     * ten. Full energy, x as 6 and the y draw as 4 give 0 + 6 + 9, fifteen,
     * the same total the thirty six year old above gets and ten less than a
     * forty six year old given the same three draws.
     */
    @Test
    fun `the long term bracket ends at forty five`() {
        assertEquals(15, injuryOutcome(45, 100, RULES, ScriptedInts(6, 4, 50)).days)
    }

    /** Past forty five a further ten days is added on top of the long term. */
    @Test
    fun `players past forty five add ten days more`() {
        assertEquals(25, injuryOutcome(46, 100, RULES, ScriptedInts(6, 4, 50)).days)
    }

    /**
     * The severity draw is checked in the spec's own order: exactly one adds
     * seventy, under four adds forty, under ten adds twenty, and anything else
     * adds nothing. Draw nought is under four but is not one, so it takes the
     * forty. A twenty five year old on full energy with x as 6 is seven days
     * before the bonus.
     */
    @Test
    fun `severity is added in the spec's order`() {
        assertEquals(7 + 70, injuryOutcome(25, 100, RULES, ScriptedInts(6, 0, 1)).days)
        assertEquals(7 + 40, injuryOutcome(25, 100, RULES, ScriptedInts(6, 0, 0)).days)
        assertEquals(7 + 40, injuryOutcome(25, 100, RULES, ScriptedInts(6, 0, 3)).days)
        assertEquals(7 + 20, injuryOutcome(25, 100, RULES, ScriptedInts(6, 0, 4)).days)
        assertEquals(7 + 20, injuryOutcome(25, 100, RULES, ScriptedInts(6, 0, 9)).days)
        assertEquals(7, injuryOutcome(25, 100, RULES, ScriptedInts(6, 0, 10)).days)
    }

    /**
     * From thirty five the player loses five strength for good. The match only
     * reports it: strength lives on the squad, which the engine cannot reach.
     */
    @Test
    fun `an injury past thirty five costs five strength for good`() {
        assertEquals(5, injuryOutcome(35, 100, RULES, ScriptedInts(6, 0, 50)).permanentStrengthLoss)
        assertEquals(0, injuryOutcome(34, 100, RULES, ScriptedInts(6, 0, 50)).permanentStrengthLoss)
    }

    /**
     * All three draws are made whatever the age, so a squad of twenty year
     * olds and a squad of forty year olds consume the same stream.
     */
    @Test
    fun `the same three draws are made at every age`() {
        for (age in listOf(18, 25, 30, 35, 40, 50)) {
            val rng = ScriptedInts(6, 4, 50)
            injuryOutcome(age, 100, RULES, rng)
            assertEquals(3, rng.draws, "age $age")
        }
    }

    private companion object {
        val RULES = RuleSets.CLASSIC
    }
}
