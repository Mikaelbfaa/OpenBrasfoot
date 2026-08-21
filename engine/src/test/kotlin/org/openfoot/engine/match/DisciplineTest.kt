package org.openfoot.engine.match

import org.openfoot.engine.world.ScriptedInts
import org.openfoot.model.Marking
import org.openfoot.model.RuleSets
import org.openfoot.model.TeamSide
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Section 3.8's per minute roll, taken apart.
 *
 * Every expectation here is recomputed from the spec's own tables while the
 * test is written, and the inputs are stated in each docstring, so a reader
 * can check the arithmetic without leaving the file.
 */
class DisciplineTest {

    /**
     * The victim draw is rand(100) > 55 for the home side. With a nought based
     * rand that is the draws 56 to 99, forty four of a hundred, which is the
     * forty four per cent section 3.8 quotes; the remaining fifty six draws,
     * nought to 55, give the away side its fifty six per cent.
     */
    @Test
    fun `the victim side is drawn at forty four to fifty six`() {
        assertEquals(TeamSide.HOME, victimSide(ScriptedInts(56), RULES))
        assertEquals(TeamSide.AWAY, victimSide(ScriptedInts(55), RULES))
        assertEquals(TeamSide.HOME, victimSide(ScriptedInts(99), RULES))
        assertEquals(TeamSide.AWAY, victimSide(ScriptedInts(0), RULES))
    }

    /**
     * The phase is counted inside the half, so a long first half does not push
     * the second half's opening minutes past a boundary. See open question 38.
     */
    @Test
    fun `the phase boundaries fall at fifteen and thirty minutes of each half`() {
        val clock = MatchClock(firstHalfMinutes = 47, secondHalfMinutes = 48)
        assertEquals(0, disciplinePhase(0, clock, RULES))
        assertEquals(0, disciplinePhase(14, clock, RULES))
        assertEquals(1, disciplinePhase(15, clock, RULES))
        assertEquals(1, disciplinePhase(29, clock, RULES))
        assertEquals(2, disciplinePhase(30, clock, RULES))
        assertEquals(2, disciplinePhase(46, clock, RULES))
        assertEquals(0, disciplinePhase(47, clock, RULES))
        assertEquals(1, disciplinePhase(47 + 15, clock, RULES))
        assertEquals(2, disciplinePhase(47 + 30, clock, RULES))
    }

    /**
     * All six half-and-phase pairs occur somewhere in a match, which is the
     * whole argument for counting the phase inside the half: counted from
     * kick off the second half would never leave phase two, and only four of
     * the six pairs would ever occur. This does not touch the threshold
     * tables themselves, only the pairs a minute can land on; see the test
     * below named for matching the spec's table cell by cell.
     */
    @Test
    fun `every half and phase combination occurs in a match`() {
        val clock = MatchClock(firstHalfMinutes = 47, secondHalfMinutes = 48)
        val reached = (0 until clock.totalMinutes)
            .map { clock.halfOf(it) to disciplinePhase(it, clock, RULES) }
            .toSet()
        assertEquals(6, reached.size)
    }

    /**
     * A second, independent transcription of section 3.8's threshold table,
     * read directly off spec/SIMULATION-SPEC.md rather than off RuleSets.kt,
     * which is the thing under test: copying the numbers from RuleSets.kt
     * here would only prove the table equals itself. That is the whole point
     * of this test; the per case tests above and below document the
     * arithmetic in prose and this one does not replace them.
     *
     * The table, transcribed from section 3.8, by phase within each half:
     *
     * First half:  yellow 70, 40, 30; red 1200, 900, 800; injury 1500, 1000, 800.
     * Second half: yellow 45, 40, 30; red 800, 700, 550; injury 800, 600, 600.
     *
     * Marking.LIGHT is used throughout, so every yellow expectation below is
     * the table's own value plus its stated thirty point relief; red and
     * injury take no relief at all.
     */
    @Test
    fun `every half and phase cell of the three tables matches the spec`() {
        data class Cell(val minute: Int, val yellow: Int, val red: Int, val injury: Int)

        val lightRelief = 30
        val cells = listOf(
            Cell(minute = 0, yellow = 70, red = 1200, injury = 1500),
            Cell(minute = 15, yellow = 40, red = 900, injury = 1000),
            Cell(minute = 30, yellow = 30, red = 800, injury = 800),
            Cell(minute = CLOCK.firstHalfMinutes, yellow = 45, red = 800, injury = 800),
            Cell(minute = CLOCK.firstHalfMinutes + 15, yellow = 40, red = 700, injury = 600),
            Cell(minute = CLOCK.firstHalfMinutes + 30, yellow = 30, red = 550, injury = 600),
        )

        for (cell in cells) {
            val thresholds = minuteThresholds(cell.minute, CLOCK, side(Marking.LIGHT), DisciplineCounts(), RULES)
            assertEquals(cell.yellow + lightRelief, thresholds.yellow, "yellow at minute ${cell.minute}")
            assertEquals(cell.red, thresholds.red, "red at minute ${cell.minute}")
            assertEquals(cell.injury, thresholds.injury, "injury at minute ${cell.minute}")
        }
    }

    /**
     * Section 3.8's first half, phase nought yellow threshold is 70, and a
     * side marking lightly adds 30, so the roll this minute is one in a
     * hundred. The red and injury thresholds take no marking relief at all.
     */
    @Test
    fun `light marking lifts the yellow threshold by thirty`() {
        val thresholds = minuteThresholds(0, CLOCK, side(Marking.LIGHT), DisciplineCounts(), RULES)
        assertEquals(70 + 30, thresholds.yellow)
        assertEquals(1200, thresholds.red)
        assertEquals(1500, thresholds.injury)
    }

    /**
     * The heaviest marking adds nothing, so a side tackling that hard runs at
     * the bare table value and is booked more than twice as often in the
     * opening quarter as one marking lightly.
     */
    @Test
    fun `the heaviest marking gets no relief at all`() {
        assertEquals(70, minuteThresholds(0, CLOCK, side(Marking.VERY_HEAVY), DisciplineCounts(), RULES).yellow)
        assertEquals(70 + 10, minuteThresholds(0, CLOCK, side(Marking.HEAVY), DisciplineCounts(), RULES).yellow)
    }

    /**
     * The second half at phase two: yellow 30, red 550, injury 600, with the
     * heaviest marking adding nothing. The minute chosen is thirty into the
     * second half of a clock whose first half ran 47 minutes.
     */
    @Test
    fun `the second half reads its own row`() {
        val late = CLOCK.firstHalfMinutes + 30
        val thresholds = minuteThresholds(late, CLOCK, side(Marking.VERY_HEAVY), DisciplineCounts(), RULES)
        assertEquals(30, thresholds.yellow)
        assertEquals(550, thresholds.red)
        assertEquals(600, thresholds.injury)
    }

    /**
     * More than five yellows doubles the threshold. Six is the first count
     * that satisfies "more than five", and the base here is 70 plus the 30 of
     * light marking.
     */
    @Test
    fun `a sixth yellow doubles the threshold`() {
        val counts = DisciplineCounts(yellows = 6)
        assertEquals((70 + 30) * 2, minuteThresholds(0, CLOCK, side(Marking.LIGHT), counts, RULES).yellow)
        assertEquals(70 + 30, minuteThresholds(0, CLOCK, side(Marking.LIGHT), DisciplineCounts(yellows = 5), RULES).yellow)
    }

    /**
     * Two sendings off replace the yellow threshold with twice the red one,
     * overwriting the doubling rather than compounding with it. At first half
     * phase nought that is 2 times 1200, so a booking goes from one minute in
     * a hundred to one in two thousand four hundred: defect 5 of section 3.15.
     */
    @Test
    fun `two sendings off overwrite the doubling`() {
        val counts = DisciplineCounts(yellows = 6, sendingsOff = 2)
        assertEquals(2 * 1200, minuteThresholds(0, CLOCK, side(Marking.LIGHT), counts, RULES).yellow)
    }

    /**
     * One injury replaces it again, with five times the injury threshold, and
     * it is applied last so it beats the red overwrite. At first half phase
     * nought that is 5 times 1500.
     */
    @Test
    fun `one injury overwrites everything before it`() {
        val counts = DisciplineCounts(yellows = 6, sendingsOff = 2, injuries = 1)
        assertEquals(5 * 1500, minuteThresholds(0, CLOCK, side(Marking.LIGHT), counts, RULES).yellow)
    }

    /**
     * The modern rules keep the doubling, which section 3.15 does not call a
     * defect, and drop both overwrites, which it does.
     */
    @Test
    fun `the modern rules drop both overwrites and keep the doubling`() {
        val counts = DisciplineCounts(yellows = 6, sendingsOff = 2, injuries = 1)
        val modern = RuleSets.MODERN
        assertEquals(
            (70 + 30) * 2,
            minuteThresholds(0, CLOCK, side(Marking.LIGHT), counts, modern).yellow,
        )
    }

    private companion object {
        val RULES = RuleSets.CLASSIC

        val CLOCK = MatchClock(firstHalfMinutes = 47, secondHalfMinutes = 48)

        fun side(marking: Marking) =
            Lineups.sideOfSlots(Lineups.FORMATION_4_4_2, strength = 50, marking = marking)
    }
}
