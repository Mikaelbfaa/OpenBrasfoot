package org.openfoot.engine.match

import org.openfoot.engine.lineup.Formations
import org.openfoot.model.Attr
import org.openfoot.model.CompetitionKind
import org.openfoot.model.Country
import org.openfoot.model.Marking
import org.openfoot.model.PlayerId
import org.openfoot.model.Position
import org.openfoot.model.Slot
import org.openfoot.model.Trait

/**
 * Fixtures for the match engine tests.
 *
 * Individual abilities are off by default, so a player's rating is exactly his
 * strength divided by ten and every expected value in a test can be worked out
 * by hand. Default characteristics are ones that carry no shooter bonus, so a
 * test that cares about bonuses has to ask for them.
 */
object Lineups {

    val NEUTRAL_TRAITS = Trait.STAMINA to Trait.CROSSING

    fun context(
        kind: CompetitionKind = CompetitionKind.FRIENDLY,
        individualAbilities: Boolean = false,
        reputation: Int = 5,
        country: Int = 65,
        continent: Int = Country.EUROPE_CONTINENT,
        isHome: Boolean = true,
        homeReputation: Int = 5,
        awayReputation: Int = 5,
    ) = StrengthContext(
        kind = kind,
        useIndividualAbilities = individualAbilities,
        sideReputation = reputation,
        sideCountry = country,
        sideContinent = continent,
        isHomeSide = isHome,
        homeReputation = homeReputation,
        awayReputation = awayReputation,
    )

    /**
     * A player who is in position for his cell unless a position is forced.
     */
    fun player(
        slot: Int,
        strength: Int,
        id: Int = slot,
        age: Int = 25,
        position: Position? = null,
        firstTrait: Trait = NEUTRAL_TRAITS.first,
        secondTrait: Trait = NEUTRAL_TRAITS.second,
        abilities: IntArray = IntArray(Attr.COUNT),
        representsSideCountry: Boolean = false,
    ): MatchPlayer {
        val cell = Slot(slot)
        return MatchPlayer(
            id = PlayerId(id),
            slot = cell,
            naturalPosition = position ?: cell.requiredPosition ?: Position.MIDFIELDER,
            age = age,
            strength = strength,
            abilities = abilities,
            firstTrait = firstTrait,
            secondTrait = secondTrait,
            representsSideCountry = representsSideCountry,
        )
    }

    fun side(
        players: List<MatchPlayer>,
        marking: Marking = Marking.LIGHT,
        context: StrengthContext = context(),
        humanManaged: Boolean = false,
    ) = MatchSide(
        lineup = players,
        marking = marking,
        context = context,
        isHumanManaged = humanManaged,
    )

    /** Builds one player per slot, all at the same strength, in list order. */
    fun sideOfSlots(
        slots: List<Int>,
        strength: Int,
        marking: Marking = Marking.LIGHT,
        context: StrengthContext = context(),
    ) = side(slots.map { player(it, strength) }, marking, context)

    /** Slot list of formation 4, the four four two the AI picks most often. */
    val FORMATION_4_4_2 = Formations.byId(4).slots.map { it.value }

    /** Slot list of formation 10, the three four three that uses slot eighteen. */
    val FORMATION_3_4_3 = Formations.byId(10).slots.map { it.value }
}
