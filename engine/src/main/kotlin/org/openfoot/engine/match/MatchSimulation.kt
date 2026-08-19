package org.openfoot.engine.match

import org.openfoot.model.Rng
import org.openfoot.model.SeedDomain
import org.openfoot.model.SpecRef
import org.openfoot.model.TeamSide
import org.openfoot.model.randRange

/**
 * One played match.
 *
 * The clock is carried because the length varies and a reader of the statistics
 * needs to know what they are out of. The starting possessor is carried because
 * possession alternates from it deterministically, so it is the only extra fact
 * needed to say whose tick any minute was.
 */
@SpecRef("3.1")
data class MatchResult(
    val clock: MatchClock,
    val stats: MatchStats,
    val homeGoals: Int,
    val awayGoals: Int,
    val startingPossessor: TeamSide,
)

/**
 * Plays a whole match.
 *
 * Section 3.1 draws who kicks off with the ball and both halves' stoppage once,
 * then walks the minutes. Section 3.5 alternates possession after every single
 * tick regardless of what happened in it, so each side is on the ball for half
 * the match and the displayed possession percentage comes from the duel counter
 * instead.
 *
 * Every minute draws from its own stream, derived from the minute index. A fork
 * depends only on the origin seed and the tag and never on how much the parent
 * has produced, so the number of draws one minute makes cannot move the next
 * one. That is what makes a match reproducible while the engine is still being
 * built: section 3.8 will add draws in the reserved discipline stream, and not
 * one draw made here will move.
 *
 * Human sides are not handled here. Section 3.1 skips automatic simulation
 * whenever a human club is involved, because the live viewer drives those tick
 * by tick, and the viewer does not exist yet.
 */
@SpecRef("3.1")
fun simulateMatch(setup: MatchSetup, rng: Rng): MatchResult {
    val matchRng = rng.fork(SeedDomain.MATCH)
    val setupRng = matchRng.fork(SETUP_STREAM)

    val startingPossessor =
        if (setupRng.randRange(0, 1) == 0) TeamSide.HOME else TeamSide.AWAY
    val clock = matchClock(setupRng)

    var stats = MatchStats()
    var possessor = startingPossessor

    for (minute in 0 until clock.totalMinutes) {
        val minuteRng = matchRng.fork(minute.toLong())

        val outcome = playTick(
            setup = setup,
            possessor = possessor,
            goalsScoredByPossessor = stats.of(possessor).goals,
            rng = minuteRng.fork(PLAY_STREAM),
        )
        stats = stats.record(outcome)
        possessor = possessor.opponent
    }

    return MatchResult(
        clock = clock,
        stats = stats,
        homeGoals = stats.home.goals,
        awayGoals = stats.away.goals,
        startingPossessor = startingPossessor,
    )
}

/**
 * Folds one tick into the running counters.
 *
 * A tackle belongs to the side that did not have the ball. Everything else
 * belongs to the side that did. The duel winner is counted separately from all
 * of it, because that is the number the original displays as possession.
 */
@SpecRef("3.13")
private fun MatchStats.record(outcome: TickOutcome): MatchStats {
    val possessor = outcome.possessor
    val defender = possessor.opponent

    var home = this.home
    var away = this.away

    fun update(side: TeamSide, change: (SideStats) -> SideStats) {
        if (side == TeamSide.HOME) home = change(home) else away = change(away)
    }

    update(outcome.possessionWinner) { it.copy(possessionsWon = it.possessionsWon + 1) }

    when (outcome.event) {
        TickEvent.GOAL -> update(possessor) {
            it.copy(goals = it.goals + 1, shots = it.shots + 1, onTarget = it.onTarget + 1)
        }

        TickEvent.SAVE -> update(possessor) {
            it.copy(shots = it.shots + 1, onTarget = it.onTarget + 1)
        }

        TickEvent.WIDE -> update(possessor) {
            it.copy(shots = it.shots + 1, wide = it.wide + 1)
        }

        TickEvent.TACKLE -> update(defender) { it.copy(tackles = it.tackles + 1) }

        TickEvent.MISPLACED_PASS -> update(possessor) {
            it.copy(misplacedPasses = it.misplacedPasses + 1)
        }
    }

    return MatchStats(home, away)
}

/** The stream the once per match draws come from. */
@SpecRef("3.1")
private const val SETUP_STREAM = 0x5E7DL

/**
 * Reserved for section 3.8, which rolls discipline, injury and substitution
 * once per minute, before that minute's tick.
 *
 * Declared internal rather than private so that allWarningsAsErrors does not
 * reject it as unused: Kotlin only warns on unused private declarations, and an
 * internal constant with nothing referencing it yet compiles clean. Claiming
 * the stream now, even unused, means filling it in later cannot move a single
 * draw the play stream makes, which is what keeps matches recorded today
 * reproducible tomorrow.
 */
@SpecRef("3.8")
internal const val DISCIPLINE_STREAM = 0xD15CL

/** The stream one tick draws from. */
@SpecRef("3.5")
private const val PLAY_STREAM = 0x71CBL
