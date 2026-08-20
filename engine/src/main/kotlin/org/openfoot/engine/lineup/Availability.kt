package org.openfoot.engine.lineup

import org.openfoot.engine.match.SideState
import org.openfoot.engine.world.Player
import org.openfoot.model.SpecRef

/**
 * What the automatic lineup of section 5.4 is allowed to know about a player
 * beyond what the player himself carries.
 *
 * Two things and no more, because step 1 filters and step 2 sorts and neither
 * reads anything else. Whether he can be picked at all covers the injured, the
 * suspended and, for a human managed club, the man whose contract has run out.
 * Energy is the only tie break in the sort, and section 3.2 is explicit that it
 * is a tie break and nothing more: the AI does not rest a tired player.
 */
@SpecRef("5.4")
data class PlayerAvailability(
    @property:SpecRef("5.4") val canPlay: Boolean,
    @property:SpecRef("3.9") val energy: Int,
)

/**
 * Answers those two questions for one player of one squad.
 *
 * The player is passed alongside his index so an implementation can key off
 * either. Season state, which is where injuries and suspensions actually live,
 * does not exist yet, so v0.1 always hands in the full squad. When it does
 * exist it supplies a real implementation and this signature does not change,
 * which is the whole reason the lineup takes an interface here rather than
 * reading a list of injured players it would have to be given.
 */
@SpecRef("5.4")
fun interface Availability {

    fun of(index: Int, player: Player): PlayerAvailability

    companion object {

        /**
         * Everybody fit, everybody fresh. What the opening day of a season
         * looks like, and what every caller in v0.1 uses.
         */
        @SpecRef("5.4")
        val FULL_SQUAD = Availability { _, _ ->
            PlayerAvailability(canPlay = true, energy = SideState.FULL_ENERGY)
        }
    }
}
