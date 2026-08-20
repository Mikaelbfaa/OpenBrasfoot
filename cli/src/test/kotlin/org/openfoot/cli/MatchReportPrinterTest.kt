package org.openfoot.cli

import org.openfoot.engine.match.MatchClock
import org.openfoot.engine.match.MatchEvent
import org.openfoot.engine.match.MatchReport
import org.openfoot.model.TeamSide
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The output is compared with a diff between runs, so nothing in it may read a
 * clock, a locale or a hash order. What is asserted here is that it says the
 * things a reader needs and that two calls on one report are identical.
 */
class MatchReportPrinterTest {

    /**
     * A match of four minutes: the home side wins three possession duels and
     * scores once, the away side wins one and shoots wide. Small enough that
     * every number in the output can be checked by hand.
     */
    private fun report(): MatchReport {
        val log = listOf(
            MatchEvent.PossessionWon(0, TeamSide.HOME),
            MatchEvent.Shot(0, TeamSide.HOME, shooter = null, onTarget = true, scored = true),
            MatchEvent.PossessionWon(1, TeamSide.AWAY),
            MatchEvent.Shot(1, TeamSide.AWAY, shooter = null, onTarget = false, scored = false),
            MatchEvent.PossessionWon(2, TeamSide.HOME),
            MatchEvent.Tackle(2, TeamSide.AWAY),
            MatchEvent.PossessionWon(3, TeamSide.HOME),
            MatchEvent.MisplacedPass(3, TeamSide.AWAY),
        )
        return MatchReport(
            clock = MatchClock(firstHalfMinutes = 2, secondHalfMinutes = 2),
            log = log,
            homeGoals = 1,
            awayGoals = 0,
            startingPossessor = TeamSide.HOME,
        )
    }

    @Test
    fun `the score line names both clubs and the score`() {
        val text = describe(report(), homeRef = "Flamengo_bra", awayRef = "Santos_bra")

        assertTrue(text.contains("Flamengo_bra"), "home club named, was:\n$text")
        assertTrue(text.contains("Santos_bra"), "away club named, was:\n$text")
        assertTrue(text.contains("1"), "home goals shown, was:\n$text")
    }

    @Test
    fun `the same report describes identically twice`() {
        val once = describe(report(), "Flamengo_bra", "Santos_bra")
        val twice = describe(report(), "Flamengo_bra", "Santos_bra")

        assertEquals(once, twice, "the output is compared with a diff between runs")
    }

    @Test
    fun `the possession line is the duel share and not the tick share`() {
        val text = describe(report(), "Flamengo_bra", "Santos_bra")

        assertTrue(
            text.contains("75"),
            "the home side won three of four duels; the tick share would be fifty, " +
                "because section 3.5 alternates unconditionally. Was:\n$text",
        )
    }

    @Test
    fun `a goalless match still prints its shot counts`() {
        val goalless = report().copy(log = report().log.filterNot { it is MatchEvent.Shot })
        val text = describe(goalless, "Flamengo_bra", "Santos_bra")

        assertTrue(text.contains("0"), "a nought is printed rather than a line omitted, was:\n$text")
    }
}
