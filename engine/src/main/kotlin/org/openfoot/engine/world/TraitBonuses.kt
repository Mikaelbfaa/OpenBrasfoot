package org.openfoot.engine.world

import org.openfoot.model.Attr
import org.openfoot.model.Position
import org.openfoot.model.Rng
import org.openfoot.model.SpecRef
import org.openfoot.model.Trait
import org.openfoot.model.rand

/**
 * Adds what a player's two characteristics are worth on top of his generated
 * abilities, in place.
 *
 * A bonus fires when either characteristic matches, so carrying the same one
 * twice is worth exactly as much as carrying it once. That is the spec being
 * explicit about a case the data does contain.
 *
 * Bonuses are applied in the order the spec paragraph lists them, which fixes
 * the draw order. Only outfield characteristics appear: the four a goalkeeper
 * can carry are worth nothing here, which is why a keeper's abilities come
 * entirely from the table of the previous step.
 *
 * The ceiling of section 4.1 is applied last. Nothing world creation can
 * produce currently reaches it, since a player carries at most two
 * characteristics and starts well short, but the ceiling is the rule and
 * growth will bring players to it later.
 */
@SpecRef("4.2")
fun applyTraitBonuses(
    abilities: IntArray,
    position: Position,
    firstTrait: Trait,
    secondTrait: Trait,
    qualitySeed: Int,
    band: Int,
    rng: Rng,
) {
    require(abilities.size == Attr.COUNT) {
        "expected ${Attr.COUNT} abilities, got ${abilities.size}"
    }

    fun has(trait: Trait) = firstTrait == trait || secondTrait == trait

    /**
     * Forwards read the club quality seed where everyone else reads the band,
     * but only for the two creative characteristics.
     */
    val creativeBase = if (position == Position.FORWARD) qualitySeed else band

    if (has(Trait.PLAYMAKING)) {
        abilities[Attr.PLAYMAKING] += creativeBase + rng.rand(5)
        abilities[Attr.PASSING] += creativeBase + rng.rand(5)
    }
    if (has(Trait.HEADING)) {
        abilities[Attr.FINISHING] += 2 + rng.rand(3)
    }
    if (has(Trait.CROSSING)) {
        abilities[Attr.PASSING] += 2 + rng.rand(3)
    }
    if (has(Trait.TACKLING)) {
        abilities[Attr.TACKLING] += band + rng.rand(3)
    }
    if (has(Trait.DRIBBLING)) {
        abilities[Attr.TECHNIQUE] += band + rng.rand(3)
    }
    if (has(Trait.FINISHING)) {
        abilities[Attr.FINISHING] += band + rng.rand(3)
    }
    if (has(Trait.MARKING)) {
        abilities[Attr.TACKLING] += band + rng.rand(5)
    }
    if (has(Trait.PASSING)) {
        abilities[Attr.PASSING] += creativeBase + rng.rand(2)
    }
    if (has(Trait.STAMINA)) {
        abilities[Attr.TACKLING] += 3 + rng.rand(3)
    }
    if (has(Trait.PACE)) {
        abilities[Attr.PACE] += qualitySeed + rng.rand(3)
    }

    for (index in abilities.indices) {
        abilities[index] = minOf(abilities[index], ABILITY_CEILING)
    }
}

/** Abilities run zero to one hundred. */
@SpecRef("4.1")
internal const val ABILITY_CEILING = 100
