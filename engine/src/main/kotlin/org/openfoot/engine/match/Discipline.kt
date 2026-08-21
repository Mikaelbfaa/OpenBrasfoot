package org.openfoot.engine.match

import org.openfoot.model.Rng
import org.openfoot.model.RuleSet
import org.openfoot.model.SpecRef
import org.openfoot.model.TeamSide
import org.openfoot.model.rand

/**
 * Which side suffers this minute's roll.
 *
 * The one thing in the whole engine that resembles refereeing bias: the away
 * side is drawn more often than the home side, which is why it collects more
 * cards and more injuries over a season without any other rule saying so.
 */
@SpecRef("3.8")
internal fun victimSide(rng: Rng, rules: RuleSet): TeamSide =
    if (rng.rand(VICTIM_DRAW_BOUND) > rules.discipline.victimHomeThreshold) {
        TeamSide.HOME
    } else {
        TeamSide.AWAY
    }

/**
 * Which of the three phases of its own half a minute sits in.
 *
 * Counted inside the half rather than from kick off. Section 3.8 gives one
 * table per half with three phases each, and counting across the match would
 * leave the second half permanently in the last phase, making four of the six
 * cells of every table unreachable. See OPEN-QUESTIONS item 38.
 */
@SpecRef("3.8")
internal fun disciplinePhase(minute: Int, clock: MatchClock, rules: RuleSet): Int {
    val intoHalf = clock.intoHalf(minute)
    return rules.discipline.phaseBounds.count { intoHalf >= it }
}

@SpecRef("3.8")
private const val VICTIM_DRAW_BOUND = 100

/** This minute's three thresholds, after every adjustment section 3.8 names. */
@SpecRef("3.8")
internal data class MinuteThresholds(val yellow: Int, val red: Int, val injury: Int)

/**
 * This minute's three card and injury thresholds for the victim's side.
 *
 * Order matters and is the spec's own: the base table cell, then the
 * victim's marking relief on the yellow threshold only, then the more than
 * five yellows doubling, then the two or more sendings off overwrite, then
 * the one or more injuries overwrite, each later step replacing rather than
 * compounding with the one before it, so a match that has already had a
 * sending off and an injury has its doubling erased rather than multiplied
 * further.
 *
 * Section 3.15 item 5 also names a branch for more than ten yellows, on top
 * of these four. It is deliberately not ported: the same item's own note
 * calls that branch unreachable in the original, so there is nothing here
 * for it to repair or reproduce. A later reader who does not find it should
 * read this as the reason, not as an oversight.
 */
@SpecRef("3.8")
internal fun minuteThresholds(
    minute: Int,
    clock: MatchClock,
    victim: MatchSide,
    counts: DisciplineCounts,
    rules: RuleSet,
): MinuteThresholds {
    val half = clock.halfOf(minute)
    val phase = disciplinePhase(minute, clock, rules)
    val rates = rules.discipline

    val red = rates.red.of(half).of(phase)
    val injury = rates.injury.of(half).of(phase)

    var yellow = rates.yellow.of(half).of(phase) + rates.markingRelief(victim.marking)
    if (counts.yellows >= rules.manyYellowsAtLeast) {
        yellow *= rules.manyYellowsFactor
    }
    if (counts.sendingsOff >= rules.manyRedsAtLeast) {
        yellow = rules.redOverwriteFactor * red
    }
    if (counts.injuries >= rules.anyInjuryAtLeast) {
        yellow = rules.injuryOverwriteFactor * injury
    }

    return MinuteThresholds(yellow, red, injury)
}
