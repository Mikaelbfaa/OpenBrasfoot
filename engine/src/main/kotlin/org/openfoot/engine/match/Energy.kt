package org.openfoot.engine.match

import org.openfoot.model.PlayerId
import org.openfoot.model.RuleSet
import org.openfoot.model.SpecRef
import org.openfoot.model.TeamSide

/**
 * What one drain costs a player of a given age.
 *
 * The brackets are inclusive upper bounds. The last entry's bound is a
 * sentinel meaning "no bound", so it always matches and encodes section 3.9's
 * "senao 5" fall through as a rule rather than as a loop falling off the end.
 */
@SpecRef("3.9")
internal fun energyCost(age: Int, rules: RuleSet): Int =
    rules.energyCostByAge.first { age <= it.first }.second

/**
 * Whether this minute is one of the drains.
 *
 * The count restarts at each half, so a long first half does not shift the
 * second half's drains. Minutes are counted from nought within the half, which
 * puts seven drains in a forty five minute half. Section 3.9 never says which
 * end to count from; nought is the only reading that reproduces the twenty
 * eight it quotes for a twenty four year old over a whole match. See
 * OPEN-QUESTIONS.
 */
@SpecRef("3.9")
internal fun drainsThisMinute(minute: Int, clock: MatchClock, rules: RuleSet): Boolean =
    clock.intoHalf(minute) % rules.energyDrainInterval == 0

/**
 * Drains everyone on the pitch, if this minute is a drain.
 *
 * The keeper is exempt for one half, which is section 3.9's rule and not an
 * oversight. Nobody on the bench is drained, because section 3.9 drains
 * players in the match and a substitute comes on with the energy he has.
 *
 * Energy never influences a probability on the pitch. Section 3.9 says so
 * outright, and section 3.3 does not take it as an input. It is read by the
 * substitution trigger and by injury severity, both of which arrive with
 * section 3.8.
 */
@SpecRef("3.9")
internal fun MatchState.drainEnergy(minute: Int, clock: MatchClock): MatchState {
    if (!drainsThisMinute(minute, clock, setup.rules)) {
        return this
    }
    return drainSide(TeamSide.HOME, minute, clock).drainSide(TeamSide.AWAY, minute, clock)
}

/**
 * Subtracts one drain's cost from every player of one side's pitch lineup,
 * flooring at nought. The keeper is skipped when this minute's half is the
 * rules' exempt half.
 */
@SpecRef("3.9")
private fun MatchState.drainSide(side: TeamSide, minute: Int, clock: MatchClock): MatchState {
    val rules = setup.rules
    val half = clock.halfOf(minute)
    val lineup = setup.side(side).lineup
    val current = of(side)

    val drained = LinkedHashMap<PlayerId, Int>()
    for ((id, energy) in current.energy) {
        drained[id] = energy
    }
    for (player in lineup) {
        if (player.slot.value == rules.keeperSlot && half == rules.keeperExemptHalf) {
            continue
        }
        val remaining = current.energy.getValue(player.id) - energyCost(player.age, rules)
        drained[player.id] = remaining.coerceAtLeast(0)
    }

    return with(side, current.copy(energy = drained))
}
