package org.openfoot.engine.match

import org.openfoot.model.Band
import org.openfoot.model.RiskGroup
import org.openfoot.model.Rng
import org.openfoot.model.RuleSet
import org.openfoot.model.SpecRef
import org.openfoot.model.bound
import org.openfoot.model.pick
import org.openfoot.model.rand

/**
 * Which risk group this minute's card or injury falls on.
 *
 * One rand against the table's own bound, then one lookup, matching the shape
 * of every other section 3.8 table.
 */
@SpecRef("3.8")
internal fun drawRiskGroup(bands: List<Band<RiskGroup>>, rng: Rng): RiskGroup =
    bands.pick(rng.rand(bands.bound()))

/**
 * Which player of the victim side the event falls on.
 *
 * Drawn uniformly among the players standing in the group's cells, not among
 * the cells themselves: a group whose range is wider than the formation uses
 * would otherwise drop most of its events. See OPEN-QUESTIONS item 40.
 *
 * Nobody in the range means nobody is picked and the event does not happen.
 * The draw is skipped in that case rather than made and discarded, so a side
 * with an unusual shape does not shift the stream for the rest of the match.
 */
@SpecRef("3.8")
internal fun drawVictim(side: MatchSide, group: RiskGroup, rules: RuleSet, rng: Rng): MatchPlayer? {
    val cells = rules.discipline.riskGroupSlots[group.ordinal]
    val candidates = side.lineup.filter { it.slot.value in cells }
    if (candidates.isEmpty()) {
        return null
    }
    return candidates[rng.rand(candidates.size)]
}
