package org.openfoot.engine.world

import org.openfoot.model.Rng
import org.openfoot.model.SpecRef
import org.openfoot.model.bfRound
import org.openfoot.model.rand

/**
 * The strength a player is born with when the world is created.
 *
 * The original stores no per player strength anywhere, so this is not a value
 * being read back: it is the moment a player acquires the number the whole
 * match engine runs on. Almost all of it comes from the club. A starter at a
 * strong club in a strong country is a strong player, and the same man listed
 * as a substitute is not.
 *
 * The youth subtraction of section 4.4 is deliberately absent. Youth squads are
 * not generated yet and the dataset carries none, so a flag with only one
 * reachable value would be dead weight. It arrives with the youth work.
 *
 * Draws happen in the order the spec lists them, which fixes the sequence for
 * anyone replaying a career: the base spread first, then the starter bonus,
 * then the star bonus.
 */
@SpecRef("4.4")
fun initialStrength(
    clubLevel: Int,
    bands: GenerationBands,
    countryLevel: Int,
    starter: Boolean,
    star: Boolean,
    rng: Rng,
): Int {
    var strength = ClubBands.mappedLevel(clubLevel) + bands.strengthBase + rng.rand(BASE_SPREAD)

    if (starter) {
        strength += STARTER_BONUS + rng.rand(STARTER_SPREAD)
    }
    if (star) {
        strength += STAR_BONUS + rng.rand(STAR_SPREAD)
    }

    val scale = countryScale(clubLevel, countryLevel)
    if (scale != 1.0) {
        strength = bfRound(strength * scale)
    }

    return minOf(strength, STRENGTH_CEILING)
}

/**
 * How much a weak federation drags its clubs down.
 *
 * Two separate ladders. A country at or below the weak threshold penalises
 * every club it holds, and the penalty eases as the club gets better. A country
 * above it penalises only clubs below level ten, and leaves the rest alone.
 *
 * Note that the lowest rungs of both ladders sit below the level range the data
 * files can express, so a club level of five or three cannot occur. They are
 * kept because the spec states them, and a test asserts they stay unreachable.
 */
@SpecRef("4.4")
internal fun countryScale(clubLevel: Int, countryLevel: Int): Double =
    if (countryLevel <= WEAK_COUNTRY_CEILING) {
        when {
            clubLevel <= 5 -> 0.40
            clubLevel < 10 -> 0.65
            else -> 0.75
        }
    } else {
        when {
            clubLevel >= 10 -> 1.0
            clubLevel < 3 -> 0.50
            clubLevel < 5 -> 0.60
            else -> 0.70
        }
    }

/** A country at or below this level penalises every club it holds. */
@SpecRef("4.4")
internal const val WEAK_COUNTRY_CEILING = 13

@SpecRef("4.4")
private const val BASE_SPREAD = 3

@SpecRef("4.4")
private const val STARTER_BONUS = 8

@SpecRef("4.4")
private const val STARTER_SPREAD = 2

@SpecRef("4.4")
private const val STAR_BONUS = 9

@SpecRef("4.4")
private const val STAR_SPREAD = 3

@SpecRef("4.4")
private const val STRENGTH_CEILING = 100
