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
 * The six cell groups section 3.8 draws a card or an injury's victim from,
 * plus the keeper as a seventh group of his own.
 *
 * Named G0 to G5 rather than after a role, because the spec itself names them
 * that way and none of the six carries a role name of its own. KEEPER is kept
 * separate from G0 through G5 rather than folded into whichever group's cell
 * range happens to include slot one, matching the spec's own goleiro branch,
 * which every one of section 3.8's three distributions tests on its own.
 */
@SpecRef("3.8")
enum class RiskGroup { G0, G1, G2, G3, G4, G5, KEEPER }

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
    @property:SpecRef("3.8") val riskGroupSlots: List<IntRange>,
    @property:SpecRef("3.8") val yellowRisk: List<Band<RiskGroup>>,
    @property:SpecRef("3.8") val redRisk: List<Band<RiskGroup>>,
    @property:SpecRef("3.8") val injuryRisk: List<Band<RiskGroup>>,
) {
    /**
     * The victim's marking relief on the yellow threshold. Indexed by ordinal,
     * like RuleSet.markingBonus, so the table stays data rather than a when
     * chain. The red and injury thresholds take no relief at all; section 3.8
     * applies this only to the yellow row.
     */
    fun markingRelief(marking: Marking): Int = yellowMarkingRelief[marking.ordinal]
}

/**
 * What one age bracket contributes to an injury's length.
 *
 * Section 3.8 writes six branches that differ in three independent ways: some
 * take the energy base and the youngest bracket does not, each adds a
 * constant of its own, and the two oldest add the long term draw on top of
 * the constant. Written out as three fields rather than six formulas, the
 * table becomes data and the arithmetic that combines them happens once, in
 * the engine's injuryOutcome.
 */
@SpecRef("3.8")
data class InjuryTerm(
    @property:SpecRef("3.8") val usesEnergyBase: Boolean,
    @property:SpecRef("3.8") val constant: Int,
    @property:SpecRef("3.8") val usesLongTerm: Boolean,
)

/**
 * Everything section 3.8's injury duration reads that no rule set changes.
 *
 * ageTerms is a draw table in shape only: it is read with Band.pick(age) and
 * never with Band.bound(), because its last band is the sentinel idiom
 * energyCostByAge already uses, reaching to Int.MAX_VALUE so that no age
 * falls through. severity is a genuine rand(100) draw table, and its bands
 * are deliberately not disjoint: section 3.8 writes it as an if chain, ==1
 * before <4 before <10, and draw nought falls through the first test and
 * lands on the second. That overlap is the spec's own and the band order is
 * load bearing; do not tidy it into a disjoint table.
 */
@SpecRef("3.8")
data class InjuryRules(
    @property:SpecRef("3.8") val energyBase: List<Band<Int>>,
    @property:SpecRef("3.8") val shortTermDraw: IntRange,
    @property:SpecRef("3.8") val longTermDraw: IntRange,
    @property:SpecRef("3.8") val longTermOffset: Int,
    @property:SpecRef("3.8") val ageTerms: List<Band<InjuryTerm>>,
    @property:SpecRef("3.8") val severity: List<Band<Int>>,
    @property:SpecRef("3.8") val permanentLossAge: Int,
    @property:SpecRef("3.8") val permanentLossAmount: Int,
    @property:SpecRef("3.8") val strengthFloor: Int,
)
