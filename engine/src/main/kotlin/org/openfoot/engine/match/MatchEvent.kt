package org.openfoot.engine.match

import org.openfoot.model.SpecRef
import org.openfoot.model.TeamSide

/**
 * One thing that happened in one minute of a match.
 *
 * The log is what a match returns, and everything else is read out of it.
 * Statistics are derived rather than accumulated beside it so that no pair of
 * counters can disagree, and the live viewer of a later version replays the
 * list rather than re-deriving a timeline the engine already knew.
 *
 * Only the events the engine can currently produce are declared. Cards,
 * injuries, substitutions and goal typing arrive with the code that produces
 * them, because a case nothing can construct is a branch nothing can test.
 */
@SpecRef("3.13")
sealed interface MatchEvent {

    /** The minute the event happened in, counted from nought. */
    val minute: Int

    /** The side the event is credited to. */
    val side: TeamSide

    /**
     * An attempt on goal.
     *
     * The shooter is null when the side had nobody eligible to shoot, which
     * section 3.6c handles with its missing shooter rating rather than by
     * cancelling the attempt.
     *
     * A scored shot must be on target. toStats reads a side's goals as a
     * subset of its shots on target, so an off target goal would let goals
     * exceed on target in the derived statistics, which section 3.13's own
     * figures never allow. events() never builds one, since GOAL always
     * pairs onTarget true with scored true, but nothing else stops a hand
     * built Shot from doing so.
     *
     * The shooter is a MatchPlayer reference, and MatchPlayer is not a data
     * class, so two of them are equal only when they are the same object. A
     * substitution builds a new MatchPlayer for the same man standing in a
     * different cell, so once substitutions exist one man can appear in this
     * log under more than one object. Anything rolling a per player figure out
     * of the log, a top scorer or a shot count, must group by shooter.id,
     * which is his index in the squad and stable for the whole match, and
     * never by the object.
     */
    @SpecRef("3.6")
    data class Shot(
        override val minute: Int,
        override val side: TeamSide,
        val shooter: MatchPlayer?,
        val onTarget: Boolean,
        val scored: Boolean,
    ) : MatchEvent {
        init {
            require(!scored || onTarget) { "a scored shot must be on target" }
        }
    }

    /** A tackle, credited to the side that did not have the ball. */
    @SpecRef("3.5")
    data class Tackle(override val minute: Int, override val side: TeamSide) : MatchEvent

    /** A misplaced pass, credited to the side that had the ball. */
    @SpecRef("3.5")
    data class MisplacedPass(override val minute: Int, override val side: TeamSide) : MatchEvent

    /**
     * A possession duel won.
     *
     * Logged separately from who had the ball, because section 3.5 alternates
     * possession unconditionally every tick while the percentage the original
     * displays counts duel wins. Folding the two together would make the
     * displayed number unrecoverable.
     */
    @SpecRef("3.5")
    data class PossessionWon(override val minute: Int, override val side: TeamSide) : MatchEvent
}

/**
 * Reads the counters of section 3.13 out of a log.
 *
 * On target is goals plus saves, so shots is always on target plus wide. Both
 * are counted rather than one derived from the other, because the original
 * counts them separately and a future defect may make them disagree.
 *
 * Fouls are never touched. Section 3.13 documents the counter as one the
 * original declares and never increments, so every match reports nought.
 */
@SpecRef("3.13")
fun List<MatchEvent>.toStats(): MatchStats {
    var home = SideStats()
    var away = SideStats()

    fun update(side: TeamSide, change: (SideStats) -> SideStats) {
        if (side == TeamSide.HOME) home = change(home) else away = change(away)
    }

    for (event in this) {
        when (event) {
            is MatchEvent.Shot -> update(event.side) {
                it.copy(
                    goals = it.goals + if (event.scored) 1 else 0,
                    shots = it.shots + 1,
                    onTarget = it.onTarget + if (event.onTarget) 1 else 0,
                    wide = it.wide + if (event.onTarget) 0 else 1,
                )
            }

            is MatchEvent.Tackle -> update(event.side) { it.copy(tackles = it.tackles + 1) }

            is MatchEvent.MisplacedPass -> update(event.side) {
                it.copy(misplacedPasses = it.misplacedPasses + 1)
            }

            is MatchEvent.PossessionWon -> update(event.side) {
                it.copy(possessionsWon = it.possessionsWon + 1)
            }
        }
    }

    return MatchStats(home, away)
}
