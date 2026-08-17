package org.openfoot.engine.world

import org.openfoot.model.Attr
import org.openfoot.model.Position
import org.openfoot.model.Rng
import org.openfoot.model.SpecRef
import org.openfoot.model.bfRound
import org.openfoot.model.rand

/**
 * The seven individual abilities, generated from the club and the player.
 *
 * The design consequence section 4.2 asks to preserve is visible in every row
 * below: only the primary ability derives from the player's own strength, and
 * every secondary derives from the club quality seed. That is why two players
 * of very different strength at the same club have nearly the same passing, and
 * why the primary is the only place individual quality shows at world creation.
 *
 * Draws run left to right in the order the spec table writes each row. The
 * original has no reproducible order to imitate, so this order is ours and
 * fixing it here is what lets a career replay. See OPEN-QUESTIONS item 10.
 *
 * Values are not capped here. Nothing these formulas can produce comes close to
 * the ceiling of one hundred; the cap belongs with the trait bonuses that can
 * actually reach it.
 */
@SpecRef("4.2")
fun generateAbilities(
    position: Position,
    style: PlayerStyle,
    strength: Int,
    qualitySeed: Int,
    band: Int,
    rng: Rng,
): IntArray {
    val abilities = IntArray(Attr.COUNT)
    val a = qualitySeed
    val b = band
    val c = a / PLAYMAKER_SEED_DIVISOR

    when (position) {
        Position.GOALKEEPER -> {
            abilities[Attr.GOALKEEPING] = strength + rng.rand(2)
            abilities[Attr.PACE] = a + rng.rand(7)
            abilities[Attr.TECHNIQUE] = a + rng.rand(4)
            abilities[Attr.PASSING] = a + rng.rand(4)
            abilities[Attr.TACKLING] = b + rng.rand(3)
            abilities[Attr.PLAYMAKING] = b + rng.rand(3)
            abilities[Attr.FINISHING] = b + rng.rand(3)
        }

        Position.CENTREBACK -> {
            abilities[Attr.TACKLING] = bfRound(strength * 0.9) + rng.rand(2)
            abilities[Attr.GOALKEEPING] = 1 + rng.rand(7)
            abilities[Attr.PACE] = a + b + rng.rand(4)
            abilities[Attr.TECHNIQUE] = a + b + rng.rand(7)
            abilities[Attr.PASSING] = a + b + rng.rand(3)
            abilities[Attr.FINISHING] = b + rng.rand(6)
            abilities[Attr.PLAYMAKING] = a + rng.rand(5)
        }

        Position.FULLBACK -> when (style) {
            PlayerStyle.OFFENSIVE -> {
                abilities[Attr.PLAYMAKING] = bfRound(strength * 0.5) + rng.rand(5)
                abilities[Attr.FINISHING] = a + b + rng.rand(4)
                abilities[Attr.PASSING] = a + c + rng.rand(3)
                abilities[Attr.TECHNIQUE] = a + c + rng.rand(7)
                abilities[Attr.TACKLING] = a + rng.rand(4)
                abilities[Attr.PACE] = a + b + rng.rand(4)
                abilities[Attr.GOALKEEPING] = 1 + rng.rand(OUTFIELD_KEEPING_SPREAD)
            }

            else -> {
                abilities[Attr.TACKLING] = bfRound(strength * 0.8) + rng.rand(6)
                abilities[Attr.FINISHING] = b + rng.rand(4)
                abilities[Attr.PASSING] = a + rng.rand(3)
                abilities[Attr.TECHNIQUE] = a + rng.rand(7)
                abilities[Attr.PLAYMAKING] = b + rng.rand(5)
                abilities[Attr.PACE] = a + b + rng.rand(6)
                abilities[Attr.GOALKEEPING] = 1 + rng.rand(4)
            }
        }

        Position.MIDFIELDER -> when (style) {
            PlayerStyle.DEFENSIVE -> {
                abilities[Attr.TACKLING] = bfRound(strength * 0.7) + rng.rand(6)
                abilities[Attr.FINISHING] = a + rng.rand(4)
                abilities[Attr.PASSING] = a + rng.rand(3)
                abilities[Attr.TECHNIQUE] = a + rng.rand(7)
                abilities[Attr.PLAYMAKING] = a + rng.rand(5)
                abilities[Attr.PACE] = a + b + rng.rand(6)
                abilities[Attr.GOALKEEPING] = 1 + rng.rand(OUTFIELD_KEEPING_SPREAD)
            }

            else -> {
                abilities[Attr.PLAYMAKING] = strength + rng.rand(2)
                abilities[Attr.FINISHING] = a + c + rng.rand(4)
                abilities[Attr.PASSING] = a + b + rng.rand(3)
                abilities[Attr.TECHNIQUE] = a + c + rng.rand(7)
                abilities[Attr.TACKLING] = a + rng.rand(4)
                abilities[Attr.PACE] = a + c + rng.rand(4)
                abilities[Attr.GOALKEEPING] = 1 + rng.rand(OUTFIELD_KEEPING_SPREAD)
            }
        }

        Position.FORWARD -> {
            abilities[Attr.FINISHING] = bfRound(strength * 0.8) + rng.rand(2)
            abilities[Attr.GOALKEEPING] = 1 + rng.rand(6)
            abilities[Attr.PACE] = a + c + rng.rand(4)
            abilities[Attr.TECHNIQUE] = a + c + rng.rand(7)
            abilities[Attr.PASSING] = a + b + rng.rand(3)
            abilities[Attr.TACKLING] = b + rng.rand(6)
            abilities[Attr.PLAYMAKING] = b + a + rng.rand(5)
        }
    }

    return abilities
}

/**
 * Section 4.2 calls this C, and only the rows that lean on a player's touch
 * rather than his club use it.
 */
@SpecRef("4.2")
private const val PLAYMAKER_SEED_DIVISOR = 3

/**
 * The spread of the keeping ability the spec omits for three rows, taken from
 * the defensive fullback row that does state one. See OPEN-QUESTIONS item 11.
 */
@SpecRef("4.2")
private const val OUTFIELD_KEEPING_SPREAD = 4
