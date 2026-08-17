package org.openfoot.engine.match

import org.openfoot.model.Attr
import org.openfoot.model.CompetitionKind
import org.openfoot.model.Country
import org.openfoot.model.Position
import org.openfoot.model.Slot
import org.openfoot.model.SpecRef
import org.openfoot.model.bfRound

/**
 * Everything outside the player himself that changes his rating for one match.
 *
 * The reputations of both sides are carried because the national cup and state
 * handicap looks at the pairing rather than at one club alone.
 */
@SpecRef("3.3")
data class StrengthContext(
    val kind: CompetitionKind,
    val useIndividualAbilities: Boolean,
    val sideReputation: Int,
    val sideCountry: Int,
    val sideContinent: Int,
    val isHomeSide: Boolean,
    val homeReputation: Int,
    val awayReputation: Int,
    val playerRepresentsSideCountry: Boolean = false,
)

/**
 * A player's rating for one match on a zero to ten scale. This is the atom the
 * whole match engine is built from.
 *
 * Energy, morale, form, age and the captaincy are deliberately not inputs. The
 * original does not consult any of them here, and adding one would change every
 * result in the game.
 *
 * The original recomputes this on every use with no cache. Memoising it per
 * tick is behaviour preserving because there is no randomness inside, but that
 * is an optimisation for later and must be measured first.
 */
@SpecRef("3.3")
fun effectiveStrength(
    strength: Int,
    attrs: IntArray,
    naturalPosition: Position,
    slot: Slot,
    context: StrengthContext,
): Double {
    var rating = if (context.useIndividualAbilities) {
        weightedAbilityTotal(attrs, slot)
    } else {
        strength
    }

    if (isOutOfPosition(naturalPosition, slot)) {
        rating = bfRound(rating * OUT_OF_POSITION_FACTOR)
    }

    if (rating < 1) {
        rating = 1
    }

    val multiplier = competitionMultiplier(context)
    if (multiplier != 1.0) {
        rating = bfRound(rating * multiplier)
    }

    return rating / SCALE_DIVISOR
}

/**
 * Sums the seven abilities under the weights of the occupied cell. Each term is
 * rounded on its own before the sum, which is what the original does and which
 * does not give the same answer as rounding the total once.
 */
@SpecRef("3.3")
internal fun weightedAbilityTotal(attrs: IntArray, slot: Slot): Int {
    require(attrs.size == Attr.COUNT) {
        "expected ${Attr.COUNT} abilities, got ${attrs.size}"
    }
    val weights = SlotWeights.forSlot(slot.value)
    var total = 0
    for (index in 0 until Attr.COUNT) {
        val weight = weights[index]
        if (weight != 0.0) {
            total += bfRound(attrs[index] * weight)
        }
    }
    return total
}

/**
 * A cell that asks for a different natural position halves the rating, whatever
 * the distance of the mismatch. A striker at centre back and a fullback at
 * centre back are penalised identically.
 *
 * A player with no cell counts as out of position.
 */
@SpecRef("3.3")
internal fun isOutOfPosition(naturalPosition: Position, slot: Slot): Boolean {
    if (slot.value <= 0) {
        return true
    }
    val required = slot.requiredPosition ?: return false
    return required != naturalPosition
}

/**
 * Handicaps applied to whole sides so that a small club fields a visibly weaker
 * team once it reaches a stage above its station.
 */
@SpecRef("3.3")
internal fun competitionMultiplier(context: StrengthContext): Double = when (context.kind) {
    CompetitionKind.NATIONAL_TEAM -> when {
        !context.playerRepresentsSideCountry -> 1.0
        context.sideReputation < 3 -> 0.65
        context.sideReputation == 3 -> 0.85
        context.sideReputation == 4 -> 0.95
        else -> 1.0
    }

    CompetitionKind.CONTINENTAL_PRIMARY -> when {
        context.sideReputation < 3 -> 0.75
        context.sideReputation == 3 -> 0.85
        context.sideCountry == Country.BRAZIL -> 0.90
        else -> 1.0
    }

    CompetitionKind.CLUB_WORLD_CUP -> when {
        context.sideReputation < 3 -> 0.55
        context.sideReputation == 3 -> 0.75
        context.sideContinent != Country.EUROPE_CONTINENT -> 0.90
        else -> 1.0
    }

    CompetitionKind.NATIONAL_LEAGUE -> when {
        context.sideReputation < 3 -> 0.85
        context.sideReputation == 3 -> 0.95
        else -> 1.0
    }

    CompetitionKind.NATIONAL_CUP, CompetitionKind.STATE -> {
        val underdogAtHome = context.homeReputation < 3 && context.awayReputation >= 3
        if (underdogAtHome && !context.isHomeSide) 0.80 else 1.0
    }

    else -> 1.0
}

@SpecRef("3.3")
private const val OUT_OF_POSITION_FACTOR = 0.5

@SpecRef("3.3")
private const val SCALE_DIVISOR = 10.0
