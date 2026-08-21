package org.openfoot.engine.match

import org.openfoot.model.RuleSets
import org.openfoot.model.SplitMix64Rng
import org.openfoot.model.TeamSide
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exact results at fixed seeds, recorded before the match loop was
 * restructured.
 *
 * This is a tripwire rather than a statement about what a correct match looks
 * like. The restructuring that follows moves the loop, the statistics and the
 * player record around without intending to change a single outcome, and a
 * whole match compared byte for byte is the only evidence strong enough to say
 * so. If one of these numbers moves, either the restructuring changed
 * behaviour or something was deliberately corrected, and either way it must be
 * argued for in a commit message rather than absorbed.
 *
 * Section 3.8 landed without moving a single figure in the first three seeds
 * below, which is worth stating because it looks at first like the roll was
 * never wired in. It was: seeds 20023 and minus seven each produce two
 * bookings and seed one produces none, and a booking changes nothing a tick
 * reads. Only a dismissal or an injury changes the eleven, and none of the
 * three seeds has one. The clock and the starting possessor could not have
 * moved in any case, since both are drawn from SETUP_STREAM and section 3.8
 * touches neither that stream nor the order it is drawn in.
 *
 * The fourth seed is the one that does have both, and it was added with
 * section 3.8 for exactly that reason: without it nothing in this file would
 * distinguish a wired chain from an unwired one.
 *
 * The bench is empty on both sides throughout, so no side ever substitutes
 * here and a dismissal or an injury leaves the side on ten for the rest of
 * the match. That is the harder case for section 3.4's fixed divisors and is
 * the one worth pinning; the substitution wiring is pinned by
 * DisciplineChainTest, which plays whole matches with a bench.
 */
class MatchGoldenVectorTest {

    private fun playAt(seed: Long): MatchReport {
        val home = Lineups.sideOfSlots(
            slots = Lineups.FORMATION_4_4_2,
            strength = 50,
            context = Lineups.context(isHome = true),
        )
        val away = Lineups.sideOfSlots(
            slots = Lineups.FORMATION_4_4_2,
            strength = 50,
            context = Lineups.context(isHome = false),
        )
        val setup = MatchSetup(home = home, away = away, season = 1, rules = RuleSets.CLASSIC)
        return simulateMatch(setup, SplitMix64Rng(seed))
    }

    @Test
    fun `seed one replays exactly`() {
        val result = playAt(1L)
        assertEquals(93, result.clock.totalMinutes, "total minutes")
        assertEquals(47, result.clock.firstHalfMinutes, "first half minutes")
        assertEquals(46, result.clock.secondHalfMinutes, "second half minutes")
        assertEquals(TeamSide.AWAY, result.startingPossessor, "starting possessor")
        assertEquals(1, result.homeGoals, "home goals")
        assertEquals(2, result.awayGoals, "away goals")
        assertEquals(
            SideStats(
                goals = 1,
                shots = 13,
                onTarget = 8,
                wide = 5,
                tackles = 17,
                misplacedPasses = 22,
                possessionsWon = 42,
                fouls = 0,
            ),
            result.stats.home,
            "home stats",
        )
        assertEquals(
            SideStats(
                goals = 2,
                shots = 15,
                onTarget = 11,
                wide = 4,
                tackles = 11,
                misplacedPasses = 15,
                possessionsWon = 51,
                fouls = 0,
            ),
            result.stats.away,
            "away stats",
        )
    }

    @Test
    fun `seed twenty thousand and twenty three replays exactly`() {
        val result = playAt(20_023L)
        assertEquals(92, result.clock.totalMinutes, "total minutes")
        assertEquals(45, result.clock.firstHalfMinutes, "first half minutes")
        assertEquals(47, result.clock.secondHalfMinutes, "second half minutes")
        assertEquals(TeamSide.AWAY, result.startingPossessor, "starting possessor")
        assertEquals(1, result.homeGoals, "home goals")
        assertEquals(1, result.awayGoals, "away goals")
        assertEquals(
            SideStats(
                goals = 1,
                shots = 11,
                onTarget = 10,
                wide = 1,
                tackles = 20,
                misplacedPasses = 18,
                possessionsWon = 47,
                fouls = 0,
            ),
            result.stats.home,
            "home stats",
        )
        assertEquals(
            SideStats(
                goals = 1,
                shots = 12,
                onTarget = 8,
                wide = 4,
                tackles = 17,
                misplacedPasses = 14,
                possessionsWon = 45,
                fouls = 0,
            ),
            result.stats.away,
            "away stats",
        )
    }

    /**
     * The seed that carries section 3.8 through a whole match.
     *
     * The away side loses its cell 13 to a second yellow in minute 74 and its
     * cell 16 to an injury in minute 81, so it plays the last nineteen minutes
     * with ten and the last twelve with nine, with an empty bench and no
     * replacement for either. Both events are asserted alongside the figures
     * so that a chain which stopped producing them, or which produced them in
     * a different minute or on a different player, fails here with something
     * a reader can act on rather than only as a moved shot count.
     *
     * The two bookings in the log are the two the cell 13 player collected,
     * since a sending off for a second yellow logs the booking as well.
     */
    @Test
    fun `seed ten replays exactly, dismissal and injury included`() {
        val result = playAt(10L)
        assertEquals(96, result.clock.totalMinutes, "total minutes")
        assertEquals(47, result.clock.firstHalfMinutes, "first half minutes")
        assertEquals(49, result.clock.secondHalfMinutes, "second half minutes")
        assertEquals(TeamSide.AWAY, result.startingPossessor, "starting possessor")
        assertEquals(2, result.homeGoals, "home goals")
        assertEquals(1, result.awayGoals, "away goals")

        val bookings = result.log.filterIsInstance<MatchEvent.Booking>()
        assertEquals(2, bookings.size, "bookings")
        assertTrue(
            bookings.all { it.side == TeamSide.AWAY && it.player.slot.value == 13 },
            "both bookings belong to the away side's cell 13: $bookings",
        )

        val dismissal = result.log.filterIsInstance<MatchEvent.SendingOff>().single()
        assertEquals(74, dismissal.minute, "the minute of the dismissal")
        assertEquals(TeamSide.AWAY, dismissal.side, "the side that lost a player")
        assertEquals(13, dismissal.player.slot.value, "the cell the dismissed player stood in")
        assertTrue(dismissal.secondYellow, "the dismissal is a second yellow")

        val injury = result.log.filterIsInstance<MatchEvent.Injury>().single()
        assertEquals(81, injury.minute, "the minute of the injury")
        assertEquals(TeamSide.AWAY, injury.side, "the side that lost a second player")
        assertEquals(16, injury.player.slot.value, "the cell the injured player stood in")
        assertEquals(7, injury.days, "the days the injury costs him")

        assertTrue(
            result.log.none { it is MatchEvent.Substitution },
            "neither side has a bench, so nobody can be replaced",
        )

        assertEquals(
            SideStats(
                goals = 2,
                shots = 21,
                onTarget = 17,
                wide = 4,
                tackles = 12,
                misplacedPasses = 11,
                possessionsWon = 56,
                fouls = 0,
            ),
            result.stats.home,
            "home stats",
        )
        assertEquals(
            SideStats(
                goals = 1,
                shots = 13,
                onTarget = 10,
                wide = 3,
                tackles = 16,
                misplacedPasses = 23,
                possessionsWon = 40,
                fouls = 0,
            ),
            result.stats.away,
            "away stats",
        )
    }

    @Test
    fun `seed minus seven replays exactly`() {
        val result = playAt(-7L)
        assertEquals(93, result.clock.totalMinutes, "total minutes")
        assertEquals(45, result.clock.firstHalfMinutes, "first half minutes")
        assertEquals(48, result.clock.secondHalfMinutes, "second half minutes")
        assertEquals(TeamSide.AWAY, result.startingPossessor, "starting possessor")
        assertEquals(2, result.homeGoals, "home goals")
        assertEquals(2, result.awayGoals, "away goals")
        assertEquals(
            SideStats(
                goals = 2,
                shots = 11,
                onTarget = 9,
                wide = 2,
                tackles = 17,
                misplacedPasses = 17,
                possessionsWon = 49,
                fouls = 0,
            ),
            result.stats.home,
            "home stats",
        )
        assertEquals(
            SideStats(
                goals = 2,
                shots = 12,
                onTarget = 11,
                wide = 1,
                tackles = 18,
                misplacedPasses = 18,
                possessionsWon = 44,
                fouls = 0,
            ),
            result.stats.away,
            "away stats",
        )
    }
}
