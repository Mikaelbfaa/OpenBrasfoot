package org.openfoot.engine.match

import org.openfoot.model.Rng
import org.openfoot.model.RuleSet
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
 * one. That guarantee is about stream position only, and section 3.8 is the
 * reason it is not the whole story: a sending off, an injury or a substitution
 * mutates the lineup, so what a later tick's duels compare changes even though
 * the stream position feeding that tick has not moved at all. Both properties
 * hold together. A match replays exactly from its seed, because every draw and
 * every lineup change is a function of that seed; and a change to section 3.8
 * moves the results of matches recorded before it, because it changes what the
 * later ticks are comparing.
 *
 * A human sided match is not routed away here. The original sends any match
 * with a human managed club to its live viewer instead of simulating it
 * automatically, and that viewer does not exist in this project yet, so
 * nothing currently calls simulateMatch that way. Calling it with a human
 * sided setup in the meantime is not nonsensical: the anti exploit rules of
 * sections 3.6b and 3.6c legitimately read MatchSetup.hasHumanSide regardless
 * of how the match was reached, so this function does not reject the case.
 *
 * The two bench parameters default to empty. A side with an empty bench is
 * legal and plays on with ten after a dismissal or an injury, which is what
 * section 3.4's fixed divisors then punish; it simply never substitutes.
 */
@SpecRef("3.1")
fun simulateMatch(
    setup: MatchSetup,
    rng: Rng,
    homeBench: List<MatchPlayer> = emptyList(),
    awayBench: List<MatchPlayer> = emptyList(),
): MatchReport {
    val played = playMatch(setup, rng, homeBench, awayBench)
    return MatchReport(
        clock = played.clock,
        log = played.state.log,
        homeGoals = played.state.homeGoals,
        awayGoals = played.state.awayGoals,
        startingPossessor = played.startingPossessor,
    )
}

/**
 * A match as it stood at the final whistle, before anything was read out of it.
 *
 * MatchReport deliberately carries only what a reader of a match needs, and
 * section 3.8's three counters are not that: they are the running totals the
 * thresholds are adjusted by, and the log is where the events themselves are
 * recorded. This value exists so that a test can still reach them and check
 * that the two agree, which is the one assertion that catches a counter
 * drifting away from the log it is supposed to summarise.
 */
@SpecRef("3.1")
internal data class PlayedMatch(
    val clock: MatchClock,
    val startingPossessor: TeamSide,
    val state: MatchState,
)

/**
 * Plays the whole match and hands back the state rather than the report.
 *
 * The draws of a match, in the order this function makes them:
 *
 * 1. the starting possessor and both halves' stoppage, from SETUP_STREAM
 * 2. each side's substitution plan, from SUBSTITUTION_PLAN_STREAM forked again
 *    by the side's ordinal, so that the home side's plan cannot move the away
 *    side's
 * 3. then every minute in turn, each from its own child of the match stream
 *
 * Forking never consumes, so none of these can shift another. That is what
 * lets section 3.8 be added without moving the clock or the kick off, both of
 * which are drawn from SETUP_STREAM exactly as they were before it landed.
 */
@SpecRef("3.1")
internal fun playMatch(
    setup: MatchSetup,
    rng: Rng,
    homeBench: List<MatchPlayer>,
    awayBench: List<MatchPlayer>,
): PlayedMatch {
    val matchRng = rng.fork(SeedDomain.MATCH)
    val setupRng = matchRng.fork(SETUP_STREAM)

    val startingPossessor =
        if (setupRng.randRange(0, 1) == 0) TeamSide.HOME else TeamSide.AWAY
    val clock = matchClock(setupRng)

    val planRng = matchRng.fork(SUBSTITUTION_PLAN_STREAM)
    var state = initialState(
        setup = setup,
        startingPossessor = startingPossessor,
        homeBench = homeBench,
        awayBench = awayBench,
        homePlan = planFor(
            side = setup.home,
            bench = homeBench,
            rng = planRng.fork(TeamSide.HOME.ordinal.toLong()),
            rules = setup.rules,
        ),
        awayPlan = planFor(
            side = setup.away,
            bench = awayBench,
            rng = planRng.fork(TeamSide.AWAY.ordinal.toLong()),
            rules = setup.rules,
        ),
    )
    for (minute in 0 until clock.totalMinutes) {
        state = playMinute(state, minute, clock, matchRng.fork(minute.toLong()))
    }

    return PlayedMatch(clock = clock, startingPossessor = startingPossessor, state = state)
}

/**
 * One side's substitution plan, or the empty plan for a side that can never
 * act on one.
 *
 * A plan is a list of minutes at which the AI means to bring a reserve on, so
 * a side that may not be substituted at all has no use for one. The condition
 * is canSubstitute, the same one runSubstitutionWindow turns a side away by
 * before it looks at the minute, called rather than restated so that the two
 * cannot drift apart. Nobody has used a substitution yet at kick off, so the
 * count this passes in is zero, which is simply true rather than a stand in
 * for anything. Skipping the draw therefore changes no result at all.
 *
 * Nor can it move any other draw. Each side's plan is drawn from a stream of
 * its own, forked from the match by SUBSTITUTION_PLAN_STREAM and again by the
 * side's ordinal, and a fork depends only on the origin seed and the tag and
 * never on how much has been taken from a sibling. Not drawing one side's plan
 * therefore leaves the other side's plan, every minute's chain and every
 * minute's tick exactly where they were.
 */
@SpecRef("3.8")
private fun planFor(
    side: MatchSide,
    bench: List<MatchPlayer>,
    rng: Rng,
    rules: RuleSet,
): SubstitutionPlan =
    if (canSubstitute(side, bench, substitutionsUsed = 0, maxPerSide = rules.substitutions.maxPerSide)) {
        substitutionPlan(rng, rules)
    } else {
        SubstitutionPlan.NONE
    }

/**
 * One minute of a match.
 *
 * Section 3.5 alternates possession after every single tick regardless of what
 * happened in it, so the alternation is unconditional and lives here rather
 * than inside the tick, which reports the duel winner instead.
 *
 * A minute is the drain, then section 3.8's roll, then the tick, which is the
 * order section 3.8 states: it runs once per minute, before the tick of play.
 * The drain comes first of the three because the roll reads energy, both for
 * the tiredness scan that picks who a routine substitution takes off and for
 * the injury duration, and section 3.9 drains the minute before either is
 * decided.
 *
 * The order of the roll and the tick is what makes a card bite in the same
 * minute it was shown: a player sent off at minute sixty is already off the
 * pitch when that minute's duels read the line aggregates. Energy influences
 * no probability the tick reads, so where the drain sits relative to the tick
 * cannot change anything on its own.
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
    val rolled = state.drainEnergy(minute, clock).disciplineMinute(minute, clock, rng)
    val possessor = rolled.possessor

    val outcome = playTick(
        setup = rolled.setup,
        possessor = possessor,
        goalsScoredByPossessor = rolled.goalsBy(possessor),
        rng = rng.fork(PLAY_STREAM),
    )

    val scored = outcome.event == TickEvent.GOAL
    return rolled.copy(
        log = rolled.log + outcome.events(minute),
        possessor = possessor.opponent,
        homeGoals = rolled.homeGoals + if (scored && possessor == TeamSide.HOME) 1 else 0,
        awayGoals = rolled.awayGoals + if (scored && possessor == TeamSide.AWAY) 1 else 0,
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
 * distinct from every other fixed stream and outside the range a minute index
 * can reach, without needing a copy of the literal in the test.
 *
 * Section 3.8 was added without touching this one draw for draw, so the clock
 * and the starting possessor of a match recorded before it are the clock and
 * the starting possessor of the same match today.
 */
@SpecRef("3.1")
internal const val SETUP_STREAM = 0x5E7DL

/**
 * The stream section 3.8's per minute chain draws from: the victim side, the
 * three rolls, the risk group, the player and an injury's duration.
 *
 * A child of the minute's own generator, taken before the tick's. Nothing in
 * the chain reads the play stream and nothing in the tick reads this one, so
 * the number of draws a card costs cannot move a duel.
 */
@SpecRef("3.8")
internal const val DISCIPLINE_STREAM = 0xD15CL

/**
 * The stream each side's substitution plan is drawn from, once per match.
 *
 * Forked off the match generator rather than off a minute's, because the plan
 * is drawn at kick off and read by every minute of the second half, and forked
 * again by the side's ordinal so that the two sides' plans are independent.
 */
@SpecRef("3.8")
internal const val SUBSTITUTION_PLAN_STREAM = 0x5B1AL

/**
 * The stream a minute's substitution windows draw from, one child per side.
 *
 * A sibling of DISCIPLINE_STREAM under the same minute rather than a child of
 * it, so that a minute in which the chain fired and a minute in which it did
 * not leave the other's draws exactly where they were.
 */
@SpecRef("3.8")
internal const val SUBSTITUTION_STREAM = 0x5BEDL

/**
 * The stream one tick draws from.
 *
 * Declared internal for the same reason as SETUP_STREAM: SeedStreamsTest reads
 * it to assert the reservation holds.
 */
@SpecRef("3.5")
internal const val PLAY_STREAM = 0x71CBL
