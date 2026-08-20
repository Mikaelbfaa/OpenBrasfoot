package org.openfoot.engine.match

import org.openfoot.model.Rng
import org.openfoot.model.SeedDomain
import org.openfoot.model.SpecRef
import org.openfoot.model.TeamSide
import org.openfoot.model.randRange

/**
 * One played match.
 *
 * The log is the record and the statistics are read out of it, so a reader who
 * wants to know what happened and a reader who wants to know how often are
 * looking at the same facts.
 *
 * The clock is carried because the length varies and a reader of the
 * statistics needs to know what they are out of. The starting possessor is
 * carried because possession alternates from it deterministically, so it is
 * the only extra fact needed to say whose tick any minute was.
 */
@SpecRef("3.1")
data class MatchReport(
    val clock: MatchClock,
    val log: List<MatchEvent>,
    val homeGoals: Int,
    val awayGoals: Int,
    val startingPossessor: TeamSide,
) {
    /**
     * Computed once here rather than on every read, because a caller that
     * prints a scoreboard asks for it repeatedly and the log is walked in
     * full each time.
     */
    @SpecRef("3.13")
    val stats: MatchStats = log.toStats()
}

/**
 * Plays a whole match.
 *
 * Section 3.1 draws who kicks off with the ball and both halves' stoppage once,
 * then walks the minutes. Section 3.5 alternates possession after every single
 * tick regardless of what happened in it, so each side is on the ball for half
 * the match and the displayed possession percentage comes from the duel counter
 * instead.
 *
 * The rng argument is a seed source, not a stream to be consumed. This function
 * forks it exactly once, at SeedDomain.MATCH, and never reads from it again;
 * every draw the match makes comes from children of that one fork. The result
 * therefore depends only on the generator's origin seed, never on how many
 * values it had already produced before this call, so passing the SAME Rng
 * instance to two calls of simulateMatch replays the identical match twice. A
 * caller that simulates several matches from one generator, such as a whole
 * round of fixtures, must fork a fresh child per match, for example
 * seasonRng.fork(matchId), rather than pass that one instance to every call, or
 * the whole round comes out as one match repeated.
 *
 * Every minute draws from its own stream, derived from the minute index. A fork
 * depends only on the origin seed and the tag and never on how much the parent
 * has produced, so the number of draws one minute makes cannot move the next
 * one. That guarantee is about stream position only, not about the whole match
 * staying reproducible forever: section 3.8 brings sendings off, injuries and
 * substitutions, which mutate the lineup and therefore the line aggregates for
 * every later tick. Once that lands, a match recorded today will not replay
 * identically, because what a later tick's duels compare will have changed
 * even though the stream position feeding that tick has not moved at all.
 *
 * A human sided match is not routed away here. The original sends any match
 * with a human managed club to its live viewer instead of simulating it
 * automatically, and that viewer does not exist in this project yet, so
 * nothing currently calls simulateMatch that way. Calling it with a human
 * sided setup in the meantime is not nonsensical: the anti exploit rules of
 * sections 3.6b and 3.6c legitimately read MatchSetup.hasHumanSide regardless
 * of how the match was reached, so this function does not reject the case.
 */
@SpecRef("3.1")
fun simulateMatch(setup: MatchSetup, rng: Rng): MatchReport {
    val matchRng = rng.fork(SeedDomain.MATCH)
    val setupRng = matchRng.fork(SETUP_STREAM)

    val startingPossessor =
        if (setupRng.randRange(0, 1) == 0) TeamSide.HOME else TeamSide.AWAY
    val clock = matchClock(setupRng)

    var state = initialState(setup, startingPossessor)
    for (minute in 0 until clock.totalMinutes) {
        state = playMinute(state, minute, clock, matchRng.fork(minute.toLong()))
    }

    return MatchReport(
        clock = clock,
        log = state.log,
        homeGoals = state.homeGoals,
        awayGoals = state.awayGoals,
        startingPossessor = startingPossessor,
    )
}

/**
 * One minute of a match.
 *
 * Section 3.5 alternates possession after every single tick regardless of what
 * happened in it, so the alternation is unconditional and lives here rather
 * than inside the tick, which reports the duel winner instead.
 *
 * Internal rather than private so a test can hand it a state from the middle
 * of a match and assert one minute of it without playing the eighty before.
 */
@SpecRef("3.5")
internal fun playMinute(
    state: MatchState,
    minute: Int,
    clock: MatchClock,
    rng: Rng,
): MatchState {
    val possessor = state.possessor

    val outcome = playTick(
        setup = state.setup,
        possessor = possessor,
        goalsScoredByPossessor = state.goalsBy(possessor),
        rng = rng.fork(PLAY_STREAM),
    )

    val scored = outcome.event == TickEvent.GOAL
    return state.copy(
        log = state.log + outcome.events(minute),
        possessor = possessor.opponent,
        homeGoals = state.homeGoals + if (scored && possessor == TeamSide.HOME) 1 else 0,
        awayGoals = state.awayGoals + if (scored && possessor == TeamSide.AWAY) 1 else 0,
    )
}

/**
 * Turns one tick into the events it produced.
 *
 * The duel winner is always logged, whatever else happened, because it is the
 * number the original displays as possession and it is decided every tick. A
 * tackle belongs to the side that did not have the ball; everything else
 * belongs to the side that did.
 *
 * Internal rather than private so a test can hand it built TickOutcome values
 * and pin the crediting rules without needing a whole match to reach every
 * combination of event and possessor.
 */
@SpecRef("3.13")
internal fun TickOutcome.events(minute: Int): List<MatchEvent> {
    val duel = MatchEvent.PossessionWon(minute, possessionWinner)
    val rest = when (event) {
        TickEvent.GOAL ->
            MatchEvent.Shot(minute, possessor, shooter, onTarget = true, scored = true)

        TickEvent.SAVE ->
            MatchEvent.Shot(minute, possessor, shooter, onTarget = true, scored = false)

        TickEvent.WIDE ->
            MatchEvent.Shot(minute, possessor, shooter, onTarget = false, scored = false)

        TickEvent.TACKLE -> MatchEvent.Tackle(minute, possessor.opponent)

        TickEvent.MISPLACED_PASS -> MatchEvent.MisplacedPass(minute, possessor)
    }
    return listOf(duel, rest)
}

/**
 * The stream the once per match draws come from.
 *
 * Declared internal, not private, so SeedStreamsTest can assert it stays
 * distinct from PLAY_STREAM and DISCIPLINE_STREAM and outside the range a
 * minute index can reach, without needing a copy of the literal in the test.
 */
@SpecRef("3.1")
internal const val SETUP_STREAM = 0x5E7DL

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

/**
 * The stream one tick draws from.
 *
 * Declared internal for the same reason as SETUP_STREAM: SeedStreamsTest reads
 * it to assert the reservation holds.
 */
@SpecRef("3.5")
internal const val PLAY_STREAM = 0x71CBL
