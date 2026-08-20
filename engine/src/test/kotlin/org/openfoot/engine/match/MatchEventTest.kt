package org.openfoot.engine.match

import org.openfoot.model.TeamSide
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The statistics of section 3.13 are read out of the log rather than counted
 * beside it, so that no pair of counters can drift apart. These cases state
 * the crediting rules the derivation has to get right: a tackle belongs to the
 * side that did not have the ball, everything else to the side that did, and
 * on target is goals plus saves so that shots is always on target plus wide.
 */
class MatchEventTest {

    private val home = TeamSide.HOME
    private val away = TeamSide.AWAY

    @Test
    fun `a goal counts as a shot on target and a goal`() {
        val stats = listOf<MatchEvent>(
            MatchEvent.Shot(minute = 3, side = home, shooter = null, onTarget = true, scored = true),
        ).toStats()

        assertEquals(1, stats.home.goals, "goals")
        assertEquals(1, stats.home.shots, "shots")
        assertEquals(1, stats.home.onTarget, "on target")
        assertEquals(0, stats.home.wide, "wide")
    }

    @Test
    fun `a save counts as a shot on target and no goal`() {
        val stats = listOf<MatchEvent>(
            MatchEvent.Shot(minute = 3, side = home, shooter = null, onTarget = true, scored = false),
        ).toStats()

        assertEquals(0, stats.home.goals, "goals")
        assertEquals(1, stats.home.shots, "shots")
        assertEquals(1, stats.home.onTarget, "on target")
        assertEquals(0, stats.home.wide, "wide")
    }

    @Test
    fun `a shot off target counts as a shot and as wide`() {
        val stats = listOf<MatchEvent>(
            MatchEvent.Shot(minute = 3, side = home, shooter = null, onTarget = false, scored = false),
        ).toStats()

        assertEquals(1, stats.home.shots, "shots")
        assertEquals(0, stats.home.onTarget, "on target")
        assertEquals(1, stats.home.wide, "wide")
    }

    @Test
    fun `a tackle is credited to the side that made it`() {
        val stats = listOf<MatchEvent>(MatchEvent.Tackle(minute = 3, side = away)).toStats()

        assertEquals(1, stats.away.tackles, "away tackles")
        assertEquals(0, stats.home.tackles, "home tackles")
    }

    @Test
    fun `fouls stay at nought because section 3 13 never increments them`() {
        val stats = listOf<MatchEvent>(
            MatchEvent.Shot(minute = 3, side = home, shooter = null, onTarget = true, scored = true),
            MatchEvent.Tackle(minute = 4, side = away),
        ).toStats()

        assertEquals(0, stats.home.fouls, "home fouls")
        assertEquals(0, stats.away.fouls, "away fouls")
    }

    @Test
    fun `an empty log gives empty statistics`() {
        assertEquals(MatchStats(), emptyList<MatchEvent>().toStats())
    }

    @Test
    fun `a scored shot that is off target is rejected`() {
        val error = runCatching {
            MatchEvent.Shot(minute = 3, side = home, shooter = null, onTarget = false, scored = true)
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException, "expected an argument error, got $error")
    }
}
