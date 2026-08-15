package org.openbrasfoot.engine.match

import org.openbrasfoot.model.Rng
import org.openbrasfoot.model.RuleSet
import org.openbrasfoot.model.SpecRef
import org.openbrasfoot.model.Trait

/**
 * Draws the player who takes the shot.
 *
 * Candidates are the outfield players actually on the pitch. The keeper and the
 * bench are excluded before weighting, not after, because the characteristic
 * bonuses are added to the cell weight: a substitute striker has a base weight
 * of zero but would still pick up four for finishing and could take shots from
 * the bench.
 *
 * Returns null when nobody is eligible, and the caller then falls back to the
 * missing shooter rating.
 */
@SpecRef("3.6")
fun selectShooter(side: MatchSide, rules: RuleSet, rng: Rng): MatchPlayer? {
    val candidates = side.lineup.filter { it.slot.value in rules.shooterEligibleSlots }
    if (candidates.isEmpty()) {
        return null
    }
    val weights = DoubleArray(candidates.size) { shooterWeight(candidates[it], rules).toDouble() }
    return candidates[weightedPick(weights, rng)]
}

/**
 * How likely a player is to be the one who shoots.
 *
 * Forwards outweigh defenders by twenty two to one before any bonus. Finishing
 * is checked first and wins outright, so a player with both finishing and
 * heading takes the finishing bonus only. A centre back who heads gets a second
 * bonus on top, which is what puts defenders on the end of set pieces.
 */
@SpecRef("3.6")
internal fun shooterWeight(player: MatchPlayer, rules: RuleSet): Int {
    val slot = player.slot.value
    var weight = rules.shooterSlotWeights.getOrElse(slot) { 0 }

    if (player.hasTrait(Trait.FINISHING)) {
        weight += rules.shooterFinishingBonus
    } else if (player.hasTrait(Trait.HEADING)) {
        weight += rules.shooterHeadingBonus
        if (slot in rules.centrebackSlots) {
            weight += rules.shooterHeadingDefenderBonus
        }
    }

    return weight
}
