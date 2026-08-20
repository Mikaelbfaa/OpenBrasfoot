package org.openfoot.engine.match

import org.openfoot.model.PlayerId
import org.openfoot.model.SpecRef
import org.openfoot.model.TeamSide

/**
 * What one side carries through a match that is not on the pitch.
 *
 * The players on the pitch live in MatchSide, because that is what every line
 * aggregate and every duel reads. What is kept here is everything the pitch
 * does not show: who is on the bench, how much energy each player has left,
 * how many times each has been booked, and how many substitutions are gone.
 *
 * Energy covers the bench as well as the pitch. A substitute comes on with the
 * energy he has, and section 3.9 is explicit that only players on the pitch
 * are drained, so the two cannot be kept in one list.
 *
 * Every map here is ordered. An unordered map would make a match depend on
 * iteration order, which is the one thing this engine may never do.
 */
@SpecRef("3.9")
data class SideState(
    val bench: List<MatchPlayer> = emptyList(),
    @property:SpecRef("3.9") val energy: Map<PlayerId, Int> = emptyMap(),
    @property:SpecRef("3.8") val bookings: Map<PlayerId, Int> = emptyMap(),
    @property:SpecRef("3.8") val substitutionsUsed: Int = 0,
) {
    companion object {
        /** Section 3.9 starts every player at a hundred. */
        @SpecRef("3.9")
        const val FULL_ENERGY = 100
    }
}

/**
 * A match in progress.
 *
 * The setup carries the two sides as they stand this minute rather than as
 * they were named, which is what lets a sending off or a substitution change
 * what a later tick compares without any formula below needing to know that
 * anything changed.
 *
 * This is a value, and a minute of play is a function from one to the next.
 * That is the same shape the statistics fold already had, and it is what lets
 * a test build a state in the middle of a match and assert one minute of it
 * without playing the eighty before.
 */
@SpecRef("3.5")
data class MatchState(
    val setup: MatchSetup,
    val home: SideState,
    val away: SideState,
    val log: List<MatchEvent> = emptyList(),
    val possessor: TeamSide,
    val homeGoals: Int = 0,
    val awayGoals: Int = 0,
) {
    fun of(side: TeamSide): SideState = if (side == TeamSide.HOME) home else away

    fun with(side: TeamSide, state: SideState): MatchState =
        if (side == TeamSide.HOME) copy(home = state) else copy(away = state)

    /**
     * How many goals the given side has scored so far.
     *
     * Carried on the state rather than counted back out of the log, because
     * section 3.6c reads this figure every tick and walking the whole log
     * each time would make one match quadratic in its own length.
     */
    @SpecRef("3.6c")
    fun goalsBy(side: TeamSide): Int = if (side == TeamSide.HOME) homeGoals else awayGoals
}

/**
 * The state a match starts in.
 *
 * Every player named, on the pitch or on the bench, starts on full energy,
 * unbooked, with every substitution still available.
 *
 * Identities must be distinct within a side, because energy and bookings are
 * keyed by them and a collision would silently give two players one record.
 * This is checked once here rather than on every write, since a squad cannot
 * gain a player mid match.
 */
@SpecRef("3.9")
fun initialState(
    setup: MatchSetup,
    startingPossessor: TeamSide,
    homeBench: List<MatchPlayer> = emptyList(),
    awayBench: List<MatchPlayer> = emptyList(),
): MatchState = MatchState(
    setup = setup,
    home = sideState(setup.home.lineup, homeBench),
    away = sideState(setup.away.lineup, awayBench),
    possessor = startingPossessor,
)

@SpecRef("3.9")
private fun sideState(onPitch: List<MatchPlayer>, bench: List<MatchPlayer>): SideState {
    val energy = LinkedHashMap<PlayerId, Int>()
    for (player in onPitch + bench) {
        require(!energy.containsKey(player.id)) {
            "two players in one squad share ${player.id}, and energy and bookings are kept by " +
                "identity, so one of them would have no record of his own"
        }
        energy[player.id] = SideState.FULL_ENERGY
    }
    return SideState(bench = bench, energy = energy)
}
