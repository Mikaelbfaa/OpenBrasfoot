package org.openfoot.validation

import org.openfoot.engine.match.MatchPlayer
import org.openfoot.engine.match.MatchSetup
import org.openfoot.engine.match.MatchSide
import org.openfoot.engine.match.StrengthContext
import org.openfoot.model.Attr
import org.openfoot.model.CompetitionKind
import org.openfoot.model.Country
import org.openfoot.model.Marking
import org.openfoot.model.PlayerId
import org.openfoot.model.PlayerStyle
import org.openfoot.model.Position
import org.openfoot.model.RuleSet
import org.openfoot.model.RuleSets
import org.openfoot.model.Side
import org.openfoot.model.Slot
import org.openfoot.model.SpecRef
import org.openfoot.model.Trait

/**
 * The pairing section 3.16 describes: two equivalent sides, neither human, on a
 * normal ground, in season one.
 *
 * Individual abilities are off, so a player's rating is his strength over ten
 * and every figure in the check can be worked out by hand.
 */
@SpecRef("3.16")
object EqualSides {

    /** Formation four, the four four two the AI picks most often. */
    @SpecRef("3.2")
    val FORMATION_4_4_2 = listOf(1, 22, 24, 11, 13, 14, 16, 2, 9, 3, 5)

    /**
     * Strength fifty, so both sides rate five out of ten on every line and the
     * duel probabilities of section 3.6 reduce to their equal sides values.
     */
    @SpecRef("3.16")
    const val EQUAL_STRENGTH = 50

    /**
     * Reputation three on both sides, so the reputation term of section 3.3
     * cancels and neither side is favoured by anything except the ground.
     */
    @SpecRef("3.3")
    const val EQUAL_REPUTATION = 3

    /** Season one, the season section 3.16 measures. */
    @SpecRef("3.16")
    const val FIRST_SEASON = 1

    /**
     * Side and style are fixed at Side.RIGHT and PlayerStyle.OFFENSIVE for
     * every one of the eleven. No figure section 3.16 measures reads either
     * property, so a constant that says nothing about the individual player is
     * an honest stand in here, unlike Lineups.player's defaults, which a
     * caller can override per player.
     */
    fun side(
        strength: Int,
        isHome: Boolean,
        formation: List<Int> = FORMATION_4_4_2,
    ): MatchSide {
        val context = StrengthContext(
            kind = CompetitionKind.NATIONAL_LEAGUE,
            useIndividualAbilities = false,
            sideReputation = EQUAL_REPUTATION,
            sideCountry = Country.BRAZIL,
            sideContinent = SOUTH_AMERICA_CONTINENT,
            isHomeSide = isHome,
            homeReputation = EQUAL_REPUTATION,
            awayReputation = EQUAL_REPUTATION,
        )
        val lineup = formation.mapIndexed { index, slot ->
            val cell = Slot(slot)
            MatchPlayer(
                id = PlayerId(index),
                slot = cell,
                naturalPosition = cell.requiredPosition ?: Position.MIDFIELDER,
                age = 25,
                strength = strength,
                abilities = IntArray(Attr.COUNT),
                firstTrait = Trait.STAMINA,
                secondTrait = Trait.CROSSING,
                side = Side.RIGHT,
                style = PlayerStyle.OFFENSIVE,
            )
        }
        return MatchSide(lineup = lineup, marking = Marking.LIGHT, context = context)
    }

    fun setup(
        homeStrength: Int = EQUAL_STRENGTH,
        awayStrength: Int = EQUAL_STRENGTH,
        season: Int = FIRST_SEASON,
        rules: RuleSet = RuleSets.CLASSIC,
    ) = MatchSetup(
        home = side(homeStrength, isHome = true),
        away = side(awayStrength, isHome = false),
        season = season,
        rules = rules,
    )

    /**
     * The only eleven man lineup that puts the defence and the attack exactly
     * on their fixed divisors: five defenders over a divisor of five and three
     * forwards over a divisor of three, which makes both line ratings equal to
     * a single player's rating.
     *
     * It costs a shorthanded midfield, which section 3.4 collapses to the
     * degenerate rating. That is harmless here because it happens to both sides
     * and the possession duel reads only the difference.
     */
    @SpecRef("3.4")
    val FORMATION_LINES_AT_DIVISOR = listOf(1, 20, 22, 24, 13, 14, 2, 9, 3, 5, 7)

    fun linesAtDivisorSetup() = MatchSetup(
        home = side(EQUAL_STRENGTH, isHome = true, formation = FORMATION_LINES_AT_DIVISOR),
        away = side(EQUAL_STRENGTH, isHome = false, formation = FORMATION_LINES_AT_DIVISOR),
        season = FIRST_SEASON,
        rules = RuleSets.CLASSIC,
    )

    /**
     * The continent both sides belong to. Only the European exemption of
     * section 3.3 reads this, and neither side is European, so the value only
     * has to be the same on both sides.
     */
    @SpecRef("3.3")
    private const val SOUTH_AMERICA_CONTINENT = 1
}
