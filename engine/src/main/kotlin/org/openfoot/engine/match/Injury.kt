package org.openfoot.engine.match

import org.openfoot.model.Rng
import org.openfoot.model.RuleSet
import org.openfoot.model.SpecRef
import org.openfoot.model.bound
import org.openfoot.model.pick
import org.openfoot.model.rand
import org.openfoot.model.randRange

/**
 * How long an injury drawn this minute keeps its victim out, and what it
 * costs him for good.
 *
 * days is what the squad reads to know when the player is available again.
 * permanentStrengthLoss is only reported here: strength lives on the squad,
 * which the engine cannot reach, so a later task is the one that applies it.
 * Section 3.8 also gives that loss a floor of one; nothing in the engine reads
 * that floor, because there is no strength here for it to bound, so it is
 * recorded here rather than as a RuleSet property nothing reads: whoever
 * applies permanentStrengthLoss to a squad's strength is the one who must
 * floor the result at one.
 */
internal data class InjuryOutcome(val days: Int, val permanentStrengthLoss: Int)

/**
 * Section 3.8's injury duration, the one place in the whole engine where
 * energy feeds back into an outcome, which section 3.9 calls out explicitly.
 *
 * The three draws, x, the long term offset and the severity, are made in that
 * order and unconditionally: every path through the age table costs the same
 * three draws, whatever the branch it takes, so the length of the random
 * stream a match consumes never depends on the ages of the players it injures.
 */
@SpecRef("3.8")
internal fun injuryOutcome(age: Int, energy: Int, rules: RuleSet, rng: Rng): InjuryOutcome {
    val injury = rules.injuryRules
    val x = rng.randRange(injury.shortTermDraw.first, injury.shortTermDraw.last)
    val y = injury.longTermOffset + rng.randRange(injury.longTermDraw.first, injury.longTermDraw.last)
    val severityBonus = injury.severity.pick(rng.rand(injury.severity.bound()))

    val term = injury.ageTerms.pick(age)
    val base = if (term.usesEnergyBase) injury.energyBase.pick(energy) else 0
    val longTerm = if (term.usesLongTerm) y else 0
    val days = base + x + term.constant + longTerm + severityBonus

    val permanentStrengthLoss = if (age >= injury.permanentLossAge) injury.permanentLossAmount else 0
    return InjuryOutcome(days, permanentStrengthLoss)
}
