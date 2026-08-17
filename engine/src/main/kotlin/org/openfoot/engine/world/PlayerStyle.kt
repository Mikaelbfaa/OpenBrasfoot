package org.openfoot.engine.world

import org.openfoot.model.Position
import org.openfoot.model.SpecRef
import org.openfoot.model.Trait

/**
 * How a player interprets his position, which the original calls his style.
 *
 * Derived once from position and characteristics and then fixed for life. It
 * decides which cells a player is eligible for, and it selects which row of the
 * attribute table of section 4.2 generates him, so two fullbacks with the same
 * strength can come out with quite different attributes.
 */
@SpecRef("4.3")
enum class PlayerStyle {
    DEFENSIVE,
    OFFENSIVE,
    WINGER,
}

/**
 * Derives the style from position and the two characteristics.
 *
 * The order of the tests matters and follows the spec exactly. A fullback with
 * both pace and marking comes out offensive, because pace is tested first,
 * while a midfielder with both comes out offensive for the opposite reason:
 * his offensive test runs first. Reordering these would change squads.
 *
 * Goalkeepers and centrebacks have no styles to choose between.
 */
@SpecRef("4.3")
fun playerStyle(position: Position, firstTrait: Trait, secondTrait: Trait): PlayerStyle {
    fun has(vararg traits: Trait): Boolean =
        traits.any { it == firstTrait || it == secondTrait }

    return when (position) {
        Position.GOALKEEPER, Position.CENTREBACK -> PlayerStyle.DEFENSIVE

        Position.FULLBACK -> when {
            has(Trait.PACE, Trait.CROSSING) -> PlayerStyle.OFFENSIVE
            has(Trait.TACKLING, Trait.MARKING) -> PlayerStyle.DEFENSIVE
            has(Trait.DRIBBLING, Trait.FINISHING, Trait.PASSING, Trait.PLAYMAKING) ->
                PlayerStyle.OFFENSIVE
            else -> PlayerStyle.DEFENSIVE
        }

        Position.MIDFIELDER -> when {
            has(Trait.PASSING, Trait.FINISHING, Trait.DRIBBLING, Trait.PLAYMAKING) ->
                PlayerStyle.OFFENSIVE
            has(Trait.TACKLING, Trait.MARKING) -> PlayerStyle.DEFENSIVE
            else -> PlayerStyle.OFFENSIVE
        }

        Position.FORWARD -> when {
            has(Trait.TACKLING, Trait.MARKING) -> PlayerStyle.DEFENSIVE
            has(Trait.DRIBBLING, Trait.PACE, Trait.CROSSING) -> PlayerStyle.WINGER
            else -> PlayerStyle.OFFENSIVE
        }
    }
}
