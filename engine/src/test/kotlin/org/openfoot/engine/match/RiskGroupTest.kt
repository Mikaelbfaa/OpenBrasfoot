package org.openfoot.engine.match

import org.openfoot.engine.world.ScriptedInts
import org.openfoot.model.Band
import org.openfoot.model.RiskGroup
import org.openfoot.model.RuleSets
import org.openfoot.model.bound
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Section 3.8's three risk distributions and the pick inside a group.
 *
 * Each expectation is recomputed from the spec's own percentages: a band's
 * width over the table's bound must equal the percentage the spec prints
 * beside it, which is what the coverage tests below check rather than
 * restating the boundaries a second time.
 */
class RiskGroupTest {

    @Test
    fun `the yellow table covers every draw exactly once`() {
        assertCovers(RULES.discipline.yellowRisk)
    }

    @Test
    fun `the red table covers every draw exactly once`() {
        assertCovers(RULES.discipline.redRisk)
    }

    @Test
    fun `the injury table covers every draw exactly once`() {
        assertCovers(RULES.discipline.injuryRisk)
    }

    /**
     * Section 3.8 prints a percentage beside every band of every table. Each
     * one is the band's width over the table's bound, so this recomputes all
     * twenty one of them from the ranges and compares against the printed
     * figure, rather than pinning boundaries one at a time.
     */
    @Test
    fun `every band is the width section 3 8 prints beside it`() {
        val yellow = RULES.discipline.yellowRisk
        val yellowShares = yellow.associate { it.value to widthPercent(it, yellow.bound()) }
        assertEquals(25.0, yellowShares.getValue(RiskGroup.G0))
        assertEquals(15.0, yellowShares.getValue(RiskGroup.G1))
        assertEquals(25.0, yellowShares.getValue(RiskGroup.G2))
        assertEquals(8.0, yellowShares.getValue(RiskGroup.G3))
        assertEquals(9.0, yellowShares.getValue(RiskGroup.G4))
        assertEquals(3.0, yellowShares.getValue(RiskGroup.KEEPER))
        assertEquals(15.0, yellowShares.getValue(RiskGroup.G5))

        val red = RULES.discipline.redRisk
        val redShares = red.associate { it.value to widthPercent(it, red.bound()) }
        assertEquals(0.5, redShares.getValue(RiskGroup.KEEPER))
        assertEquals(39.5, redShares.getValue(RiskGroup.G0))
        assertEquals(15.0, redShares.getValue(RiskGroup.G1))
        assertEquals(25.0, redShares.getValue(RiskGroup.G2))
        assertEquals(5.0, redShares.getValue(RiskGroup.G3))
        assertEquals(10.0, redShares.getValue(RiskGroup.G4))
        assertEquals(5.0, redShares.getValue(RiskGroup.G5))

        val injury = RULES.discipline.injuryRisk
        val injuryShares = injury.associate { it.value to widthPercent(it, injury.bound()) }
        assertEquals(0.2, injuryShares.getValue(RiskGroup.KEEPER))
        assertEquals(29.8, injuryShares.getValue(RiskGroup.G0))
        assertEquals(20.0, injuryShares.getValue(RiskGroup.G1))
        assertEquals(14.0, injuryShares.getValue(RiskGroup.G2))
        assertEquals(8.0, injuryShares.getValue(RiskGroup.G3))
        assertEquals(12.0, injuryShares.getValue(RiskGroup.G4))
        assertEquals(16.0, injuryShares.getValue(RiskGroup.G5))
    }

    /**
     * Section 3.8's Grupos de risco line, transcribed once here independently
     * of RuleSets.kt, which is the thing under test: g0 through g5 in order,
     * plus the keeper's cell one. A test that only ever fields a four four
     * two never puts a candidate in g2, g3 or g4 at all, so nothing else in
     * this file would notice one of those three ranges typed wrong, or an
     * end of g0 or g5 that only happens to agree with where a four four two
     * puts its own players.
     */
    @Test
    fun `the risk group cells match section 3 8's own line`() {
        assertEquals(
            listOf(10..13, 14..17, 3..8, 2..3, 8..9, 19..24, 1..1),
            RULES.discipline.riskGroupSlots,
        )
    }

    @Test
    fun `a group draw reads one value from the table`() {
        val rng = ScriptedInts(0)
        assertEquals(RiskGroup.KEEPER, drawRiskGroup(RULES.discipline.redRisk, rng))
        assertEquals(1, rng.draws)
    }

    /**
     * A four four two puts holding midfielders in cells 11 and 13, so group
     * nought, cells 10 to 13, holds two of them and the pick chooses between
     * exactly those two.
     */
    @Test
    fun `the victim is drawn among the players standing in the group's cells`() {
        val side = Lineups.sideOfSlots(Lineups.FORMATION_4_4_2, strength = 50)
        val first = drawVictim(side, RiskGroup.G0, RULES, ScriptedInts(0))
        val second = drawVictim(side, RiskGroup.G0, RULES, ScriptedInts(1))
        assertTrue(first!!.slot.value in 10..13)
        assertTrue(second!!.slot.value in 10..13)
        assertTrue(first.id != second.id)
    }

    /**
     * The keeper group is cell one alone, so it always yields the same man and
     * never anybody else.
     */
    @Test
    fun `the keeper group yields the keeper`() {
        val side = Lineups.sideOfSlots(Lineups.FORMATION_4_4_2, strength = 50)
        assertEquals(1, drawVictim(side, RiskGroup.KEEPER, RULES, ScriptedInts(0))!!.slot.value)
    }

    /**
     * A group whose cells nobody occupies yields nobody, and the event it was
     * drawn for does not happen. See open question 40.
     */
    @Test
    fun `a group with nobody in it yields nobody`() {
        val side = Lineups.sideOfSlots(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 11, 12), strength = 50)
        assertNull(drawVictim(side, RiskGroup.G5, RULES, ScriptedInts()))
    }

    private companion object {
        val RULES = RuleSets.CLASSIC

        fun assertCovers(bands: List<Band<RiskGroup>>) {
            for (draw in 0 until bands.bound()) {
                assertEquals(1, bands.count { draw in it.draws }, "draw $draw")
            }
        }

        fun widthPercent(band: Band<RiskGroup>, bound: Int): Double =
            100.0 * (band.draws.last - band.draws.first + 1) / bound
    }
}
