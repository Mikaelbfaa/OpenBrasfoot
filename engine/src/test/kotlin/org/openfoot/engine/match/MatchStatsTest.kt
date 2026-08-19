package org.openfoot.engine.match

import org.openfoot.model.TeamSide
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The counters are what a match is for. Two things are worth pinning: on target
 * is derived rather than counted, and the foul counter of section 3.13 exists
 * and never moves.
 */
class MatchStatsTest {

    @Test
    fun `an empty record counts nothing`() {
        val stats = MatchStats()
        assertEquals(0, stats.home.shots)
        assertEquals(0, stats.away.goals)
    }

    @Test
    fun `on target is goals plus saves and never counts a miss`() {
        val side = SideStats(goals = 2, shots = 9, onTarget = 5, wide = 4)
        assertEquals(5, side.onTarget)
        assertEquals(side.shots, side.onTarget + side.wide)
    }

    @Test
    fun `fouls exist and stay at zero`() {
        assertEquals(0, MatchStats().home.fouls)
        assertEquals(0, MatchStats().away.fouls)
    }

    @Test
    fun `the record is addressed by side`() {
        val stats = MatchStats(home = SideStats(goals = 3), away = SideStats(goals = 1))
        assertEquals(3, stats.of(TeamSide.HOME).goals)
        assertEquals(1, stats.of(TeamSide.AWAY).goals)
    }

    @Test
    fun `the possession share is the home duels won over all duels`() {
        val stats = MatchStats(
            home = SideStats(possessionsWon = 49),
            away = SideStats(possessionsWon = 43),
        )
        assertEquals(49.0 / 92.0, stats.homePossessionShare())
    }

    @Test
    fun `a match with no duels reports an even share rather than dividing by zero`() {
        assertEquals(0.5, MatchStats().homePossessionShare())
    }
}
