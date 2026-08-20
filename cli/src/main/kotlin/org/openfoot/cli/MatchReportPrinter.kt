package org.openfoot.cli

import org.openfoot.engine.match.MatchReport
import org.openfoot.model.TeamSide
import kotlin.math.roundToInt

/**
 * Describes a played match in a form that is the same on every run.
 *
 * Nothing here reads a clock, a locale or a hash order, matching summarise:
 * the whole point of a match report is that it can be diffed, whether between
 * two runs of the same match or between two calls on the same report.
 *
 * Prints, in order: the two clubs and the score, the length in minutes,
 * possession as the duel share rather than the tick share (section 3.5
 * alternates possession unconditionally every tick, so the tick share is
 * always fifty percent and would say nothing), then each side's shots, on
 * target, wide, tackles and misplaced passes. Nothing here is sorted by
 * anything that could vary between two otherwise identical runs.
 */
internal fun describe(report: MatchReport, homeRef: String, awayRef: String): String {
    val stats = report.stats
    val home = stats.of(TeamSide.HOME)
    val away = stats.of(TeamSide.AWAY)
    val homePossession = (stats.homePossessionShare() * PERCENT_SCALE).roundToInt()
    val awayPossession = PERCENT_TOTAL - homePossession

    val builder = StringBuilder()
    builder.appendLine("home       $homeRef")
    builder.appendLine("away       $awayRef")
    builder.appendLine("score      ${report.homeGoals} x ${report.awayGoals}")
    builder.appendLine("minutes    ${report.clock.totalMinutes}")
    builder.appendLine("possession home $homePossession%  away $awayPossession%")
    builder.appendLine(sideLine(homeRef, home.shots, home.onTarget, home.wide, home.tackles, home.misplacedPasses))
    builder.appendLine(sideLine(awayRef, away.shots, away.onTarget, away.wide, away.tackles, away.misplacedPasses))
    return builder.toString()
}

private fun sideLine(
    ref: String,
    shots: Int,
    onTarget: Int,
    wide: Int,
    tackles: Int,
    misplacedPasses: Int,
): String =
    "  $ref  shots $shots  on target $onTarget  wide $wide  tackles $tackles  " +
        "misplaced passes $misplacedPasses"

/**
 * Turns MatchStats.homePossessionShare's zero to one fraction into a whole
 * percentage for display.
 *
 * Not annotated with SpecRef. That annotation documents where a magic number
 * in the simulation comes from, per its own docstring, and this is not a
 * simulation quantity at all: the fraction it scales is already the number
 * section 3.5 defines, and one hundred is only how a fraction becomes a
 * percentage on a terminal. There is no spec section to cite for it.
 */
private const val PERCENT_SCALE = 100.0

/**
 * The two sides' displayed possession percentages are read off as home and
 * one hundred minus home, so they always print as a pair that sums to a
 * whole match, the same reasoning as PERCENT_SCALE above: a display constant
 * with no simulation meaning of its own, so no spec section applies to it.
 */
private const val PERCENT_TOTAL = 100
