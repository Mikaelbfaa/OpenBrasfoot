package org.openfoot.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The cascade and the relaxation passes of section 5.4, tested here rather
 * than beside the automatic lineup because two callers now read them: the
 * lineup, which fills a cell from a squad, and section 3.8's substitution,
 * which fills a vacated cell from a bench.
 */
class SlotFitTest {

    private data class Candidate(
        override val position: Position,
        override val side: Side,
        override val style: PlayerStyle,
    ) : SlotCandidate

    @Test
    fun `every cascade starts with the position the cell asked for`() {
        for (position in Position.entries) {
            assertEquals(position, POSITION_CASCADE.getValue(position).first())
        }
    }

    @Test
    fun `every cascade names all five positions exactly once`() {
        for (position in Position.entries) {
            val chain = POSITION_CASCADE.getValue(position)
            assertEquals(Position.entries.size, chain.size, "$position")
            assertEquals(Position.entries.toSet(), chain.toSet(), "$position")
        }
    }

    @Test
    fun `every outfield cascade ends with the keeper`() {
        for (position in Position.entries - Position.GOALKEEPER) {
            assertEquals(Position.GOALKEEPER, POSITION_CASCADE.getValue(position).last())
        }
    }

    /**
     * Cell 2 asks for the right flank and for no sub role, so the exact pass
     * rejects a left sided fullback and the next pass, which gives up on the
     * side, accepts him.
     */
    @Test
    fun `the exact pass demands the side`() {
        val cell = Slot(2)
        val left = Candidate(Position.FULLBACK, Side.LEFT, PlayerStyle.OFFENSIVE)
        assertFalse(fits(cell, left, pass = 0))
        assertTrue(fits(cell, left, pass = 1))
    }

    /**
     * Cell 11 asks for the defensive reading of a midfielder and for no side,
     * so the second pass, which only gives up on the side, still rejects an
     * offensive one and only the third accepts him.
     */
    @Test
    fun `the second pass still demands the sub role`() {
        val cell = Slot(11)
        val offensive = Candidate(Position.MIDFIELDER, Side.RIGHT, PlayerStyle.OFFENSIVE)
        assertFalse(fits(cell, offensive, pass = 1))
        assertTrue(fits(cell, offensive, pass = 2))
    }

    /**
     * Cell 9 asks for a left sided fullback. The list order is deliberately
     * wrong sided first, so a function that took the first fullback rather
     * than the first fitting fullback would pass the wrong one back.
     */
    @Test
    fun `a cell takes the first candidate that fits it exactly`() {
        val candidates = listOf(
            Candidate(Position.FULLBACK, Side.RIGHT, PlayerStyle.OFFENSIVE),
            Candidate(Position.FULLBACK, Side.LEFT, PlayerStyle.OFFENSIVE),
        )
        assertEquals(candidates[1], chooseCandidate(Slot(9), candidates, RuleSets.CLASSIC))
    }

    /**
     * Cell 2 asks for the fullback reading on the right flank, with no sub
     * role. The list order is deliberately hostile: first a centre back on
     * the right flank, a candidate of the wrong position but that would pass
     * fits() outright at pass 0 if the search ever tried a candidate without
     * first checking his position; second a fullback on the left flank, the
     * right position but the wrong flank, who only fits once the side is
     * given up on at pass 1. The cascade's outer loop must exhaust every pass
     * of the position the cell actually asked for before it ever tries
     * another position, so the second candidate wins despite being listed
     * after the first and despite fitting the cell less exactly.
     */
    @Test
    fun `position is tried to exhaustion before the cascade moves past it`() {
        val cell = Slot(2)
        val wrongPositionRightFlank = Candidate(Position.CENTREBACK, Side.RIGHT, PlayerStyle.OFFENSIVE)
        val rightPositionWrongFlank = Candidate(Position.FULLBACK, Side.LEFT, PlayerStyle.DEFENSIVE)
        val candidates = listOf(wrongPositionRightFlank, rightPositionWrongFlank)
        assertEquals(rightPositionWrongFlank, chooseCandidate(cell, candidates, RuleSets.CLASSIC))
    }

    @Test
    fun `a cell with nobody left at all gets nobody`() {
        assertNull(chooseCandidate(Slot(1), emptyList<Candidate>(), RuleSets.CLASSIC))
    }

    /**
     * A cell that is not on the pitch asks for no position, so the cascade is
     * skipped entirely and the catch all hands back whoever is first.
     */
    @Test
    fun `a cell that asks for no position takes whoever is first`() {
        val candidates = listOf(Candidate(Position.FORWARD, Side.RIGHT, PlayerStyle.WINGER))
        assertEquals(
            candidates[0],
            chooseCandidate(Slot.UNUSED_SUBSTITUTE, candidates, RuleSets.CLASSIC),
        )
    }
}
