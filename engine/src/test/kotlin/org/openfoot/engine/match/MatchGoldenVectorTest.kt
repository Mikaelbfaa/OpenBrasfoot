package org.openfoot.engine.match

import org.openfoot.model.RuleSets
import org.openfoot.model.SplitMix64Rng
import org.openfoot.model.TeamSide
import kotlin.test.Test
import kotlin.test.assertEquals

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
 */
class MatchGoldenVectorTest {

    private fun playAt(seed: Long): MatchResult {
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
