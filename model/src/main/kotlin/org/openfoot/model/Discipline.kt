package org.openfoot.model

/**
 * One row of a section 3.8 table: the three phases of one half.
 *
 * A value in this table is the N of the spec's rand(N) == 1, so it is a
 * denominator and a larger number means a rarer event.
 */
@SpecRef("3.8")
data class PhaseThresholds(val early: Int, val middle: Int, val late: Int) {
    fun of(phase: Int): Int = when (phase) {
        0 -> early
        1 -> middle
        else -> late
    }
}

/** One whole section 3.8 table: three phases in each of the two halves. */
@SpecRef("3.8")
data class HalfThresholds(val firstHalf: PhaseThresholds, val secondHalf: PhaseThresholds) {
    fun of(half: Half): PhaseThresholds = if (half == Half.FIRST) firstHalf else secondHalf
}

/**
 * Everything section 3.8's per minute roll reads that no rule set changes.
 *
 * Grouped rather than flat, unlike most of RuleSet, because none of it is a
 * lever: the two documented defects of section 3.15 item 5 are the threshold
 * overwrites, which stay flat properties of RuleSet so that a MODERN delta is
 * still one named argument.
 */
@SpecRef("3.8")
data class DisciplineRates(
    @property:SpecRef("3.8") val victimHomeThreshold: Int,
    @property:SpecRef("3.8") val phaseBounds: List<Int>,
    @property:SpecRef("3.8") val yellow: HalfThresholds,
    @property:SpecRef("3.8") val red: HalfThresholds,
    @property:SpecRef("3.8") val injury: HalfThresholds,
    @property:SpecRef("3.12") val yellowMarkingRelief: List<Int>,
) {
    /**
     * The victim's marking relief on the yellow threshold. Indexed by ordinal,
     * like RuleSet.markingBonus, so the table stays data rather than a when
     * chain. The red and injury thresholds take no relief at all; section 3.8
     * applies this only to the yellow row.
     */
    fun markingRelief(marking: Marking): Int = yellowMarkingRelief[marking.ordinal]
}
