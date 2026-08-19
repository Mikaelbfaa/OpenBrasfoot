package org.openfoot.engine.match

import org.openfoot.model.RuleSets
import org.openfoot.model.SplitMix64Rng
import org.openfoot.model.TeamSide
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The loop is exercised with a real generator, because what is worth asserting
 * here is that it holds together over a whole match: the counters agree with
 * each other, the length obeys the clock, and the same seed gives the same
 * match.
 */
class MatchSimulationTest {

    private fun setup(homeStrength: Int = 50, awayStrength: Int = 50, season: Int = 1) =
        MatchSetup(
            home = Lineups.sideOfSlots(
                Lineups.FORMATION_4_4_2,
                homeStrength,
                context = Lineups.context(isHome = true),
            ),
            away = Lineups.sideOfSlots(
                Lineups.FORMATION_4_4_2,
                awayStrength,
                context = Lineups.context(isHome = false),
            ),
            season = season,
            rules = RuleSets.CLASSIC,
        )

    @Test
    fun `the same seed plays the same match`() {
        val first = simulateMatch(setup(), SplitMix64Rng(7))
        val second = simulateMatch(setup(), SplitMix64Rng(7))
        assertEquals(first, second)
    }

    @Test
    fun `a different seed plays a different match`() {
        val results = (1L..40L).map { simulateMatch(setup(), SplitMix64Rng(it)) }
        assertTrue(results.toSet().size > 1, "forty seeds produced one identical match")
    }

    @Test
    fun `the match runs exactly as many ticks as the clock says`() {
        repeat(200) { seed ->
            val result = simulateMatch(setup(), SplitMix64Rng(seed.toLong()))
            val ticks = result.stats.home.possessionsWon + result.stats.away.possessionsWon
            assertEquals(result.clock.totalMinutes, ticks, "seed $seed")
        }
    }

    @Test
    fun `every match lasts between ninety one and ninety seven minutes`() {
        repeat(500) { seed ->
            val minutes = simulateMatch(setup(), SplitMix64Rng(seed.toLong())).clock.totalMinutes
            assertTrue(minutes in 91..97, "seed $seed ran $minutes minutes")
        }
    }

    @Test
    fun `shots equal on target plus wide for both sides`() {
        repeat(200) { seed ->
            val stats = simulateMatch(setup(), SplitMix64Rng(seed.toLong())).stats
            listOf(stats.home, stats.away).forEach { side ->
                assertEquals(side.shots, side.onTarget + side.wide, "seed $seed")
            }
        }
    }

    @Test
    fun `goals never exceed shots on target`() {
        repeat(200) { seed ->
            val stats = simulateMatch(setup(), SplitMix64Rng(seed.toLong())).stats
            assertTrue(stats.home.goals <= stats.home.onTarget, "seed $seed")
            assertTrue(stats.away.goals <= stats.away.onTarget, "seed $seed")
        }
    }

    @Test
    fun `the scoreline agrees with the counters`() {
        repeat(200) { seed ->
            val result = simulateMatch(setup(), SplitMix64Rng(seed.toLong()))
            assertEquals(result.stats.home.goals, result.homeGoals, "seed $seed")
            assertEquals(result.stats.away.goals, result.awayGoals, "seed $seed")
        }
    }

    @Test
    fun `each side is possessor for half the ticks give or take one`() {
        repeat(200) { seed ->
            val result = simulateMatch(setup(), SplitMix64Rng(seed.toLong()))
            val total = result.clock.totalMinutes
            val homeTicks = (0 until total).count { minute ->
                possessorAt(result.startingPossessor, minute) == TeamSide.HOME
            }
            assertTrue(
                homeTicks * 2 in (total - 1)..(total + 1),
                "seed $seed gave the home side $homeTicks of $total ticks",
            )
        }
    }

    @Test
    fun `no side ever counts more than the whole match in tackles and passes`() {
        repeat(200) { seed ->
            val result = simulateMatch(setup(), SplitMix64Rng(seed.toLong()))
            val events = with(result.stats) {
                home.shots + away.shots +
                    home.tackles + away.tackles +
                    home.misplacedPasses + away.misplacedPasses
            }
            assertEquals(result.clock.totalMinutes, events, "seed $seed")
        }
    }

    @Test
    fun `fouls stay at zero over a whole match`() {
        val stats = simulateMatch(setup(), SplitMix64Rng(3)).stats
        assertEquals(0, stats.home.fouls)
        assertEquals(0, stats.away.fouls)
    }

    @Test
    fun `a much stronger side outshoots a much weaker one over many matches`() {
        val strong = (1L..300L).sumOf {
            simulateMatch(setup(homeStrength = 90, awayStrength = 30), SplitMix64Rng(it))
                .stats.home.shots
        }
        val weak = (1L..300L).sumOf {
            simulateMatch(setup(homeStrength = 90, awayStrength = 30), SplitMix64Rng(it))
                .stats.away.shots
        }
        assertTrue(strong > weak, "strong side took $strong shots against $weak")
    }
}

private fun possessorAt(starting: TeamSide, minute: Int): TeamSide =
    if (minute % 2 == 0) starting else starting.opponent
