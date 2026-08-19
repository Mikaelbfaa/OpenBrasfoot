package org.openfoot.engine.match

import org.openfoot.model.SpecRef
import org.openfoot.model.TeamSide

/**
 * What one side did in one match.
 *
 * On target is goals plus saves, so shots is always on target plus wide. Both
 * are stored rather than derived because the original counts them separately
 * and a future defect may make them disagree.
 *
 * Fouls are section 3.13's documented dead counter. The original has the field
 * and never increments it, so every match reports nought to nought fouls. It is
 * carried so that a reader of the statistics is not left wondering whether the
 * line is missing or genuinely zero.
 */
@SpecRef("3.13")
data class SideStats(
    val goals: Int = 0,
    val shots: Int = 0,
    val onTarget: Int = 0,
    val wide: Int = 0,
    val tackles: Int = 0,
    val misplacedPasses: Int = 0,
    val possessionsWon: Int = 0,
    @property:SpecRef("3.13") val fouls: Int = 0,
)

/**
 * Both sides' counters for one match.
 *
 * Immutable and rebuilt per tick. A match is under a hundred ticks, so the
 * copying costs nothing measurable, and it keeps the simulation a fold over the
 * minutes rather than a mutation of shared state.
 */
@SpecRef("3.13")
data class MatchStats(
    val home: SideStats = SideStats(),
    val away: SideStats = SideStats(),
) {
    fun of(side: TeamSide): SideStats = if (side == TeamSide.HOME) home else away

    /**
     * The share of possession duels the home side won.
     *
     * This is the number the original displays as possession percent, and it is
     * not the share of ticks a side was on the ball. Possession alternates
     * unconditionally, so that share is always one half. See section 3.5.
     */
    @SpecRef("3.5")
    fun homePossessionShare(): Double {
        val duels = home.possessionsWon + away.possessionsWon
        return if (duels == 0) EVEN_SHARE else home.possessionsWon.toDouble() / duels
    }

    private companion object {
        const val EVEN_SHARE = 0.5
    }
}
