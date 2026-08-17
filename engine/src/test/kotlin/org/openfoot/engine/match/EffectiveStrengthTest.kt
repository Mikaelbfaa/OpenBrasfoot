package org.openfoot.engine.match

import org.openfoot.model.Attr
import org.openfoot.model.CompetitionKind
import org.openfoot.model.Position
import org.openfoot.model.Slot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Golden vectors for spec section 3.3. Every expected value below is computed
 * by hand from the spec tables, never captured from this implementation.
 */
class EffectiveStrengthTest {

    private fun context(
        kind: CompetitionKind = CompetitionKind.FRIENDLY,
        individual: Boolean = false,
        reputation: Int = 5,
        country: Int = 65,
        continent: Int = EUROPE_CONTINENT,
        isHome: Boolean = true,
        homeReputation: Int = 5,
        awayReputation: Int = 5,
        represents: Boolean = false,
    ) = StrengthContext(
        kind = kind,
        useIndividualAbilities = individual,
        sideReputation = reputation,
        sideCountry = country,
        sideContinent = continent,
        isHomeSide = isHome,
        homeReputation = homeReputation,
        awayReputation = awayReputation,
        playerRepresentsSideCountry = represents,
    )

    private fun abilities(
        goalkeeping: Int = 0,
        pace: Int = 0,
        technique: Int = 0,
        passing: Int = 0,
        tackling: Int = 0,
        playmaking: Int = 0,
        finishing: Int = 0,
    ) = IntArray(Attr.COUNT).also {
        it[Attr.GOALKEEPING] = goalkeeping
        it[Attr.PACE] = pace
        it[Attr.TECHNIQUE] = technique
        it[Attr.PASSING] = passing
        it[Attr.TACKLING] = tackling
        it[Attr.PLAYMAKING] = playmaking
        it[Attr.FINISHING] = finishing
    }

    @Test
    fun `without individual abilities the rating is simply strength over ten`() {
        val actual = effectiveStrength(
            strength = 70,
            attrs = abilities(),
            naturalPosition = Position.GOALKEEPER,
            slot = Slot(1),
            context = context(),
        )
        assertEquals(7.0, actual, 1e-9)
    }

    @Test
    fun `playing out of position halves the rating`() {
        val actual = effectiveStrength(
            strength = 70,
            attrs = abilities(),
            naturalPosition = Position.FORWARD,
            slot = Slot(1),
            context = context(),
        )
        assertEquals(3.5, actual, 1e-9)
    }

    @Test
    fun `a player with no cell counts as out of position`() {
        val actual = effectiveStrength(
            strength = 70,
            attrs = abilities(),
            naturalPosition = Position.FORWARD,
            slot = Slot.NONE,
            context = context(),
        )
        assertEquals(3.5, actual, 1e-9)
    }

    @Test
    fun `wrong side of the pitch carries no penalty at all`() {
        val left = effectiveStrength(70, abilities(), Position.FULLBACK, Slot(2), context())
        val right = effectiveStrength(70, abilities(), Position.FULLBACK, Slot(9), context())
        assertEquals(left, right, 1e-9)
        assertEquals(7.0, left, 1e-9)
    }

    @Test
    fun `keeper weights follow the spec row`() {
        val actual = effectiveStrength(
            strength = 0,
            attrs = abilities(goalkeeping = 80, pace = 50, technique = 50, passing = 30),
            naturalPosition = Position.GOALKEEPER,
            slot = Slot(1),
            context = context(individual = true),
        )
        assertEquals(6.7, actual, 1e-9)
    }

    @Test
    fun `each weighted term is rounded before the sum`() {
        val attrs = abilities(goalkeeping = 80, pace = 50, technique = 50, passing = 30)
        assertEquals(67, weightedAbilityTotal(attrs, Slot(1)))
    }

    @Test
    fun `centre forward weights follow the spec row`() {
        val actual = effectiveStrength(
            strength = 0,
            attrs = abilities(pace = 60, technique = 60, passing = 40, playmaking = 40, finishing = 90),
            naturalPosition = Position.FORWARD,
            slot = Slot(20),
            context = context(individual = true),
        )
        assertEquals(7.0, actual, 1e-9)
    }

    @Test
    fun `slot eighteen still rates a player even though it feeds no aggregate`() {
        val actual = effectiveStrength(
            strength = 0,
            attrs = abilities(pace = 80, technique = 60, passing = 60, playmaking = 40, finishing = 80),
            naturalPosition = Position.FORWARD,
            slot = Slot(18),
            context = context(individual = true),
        )
        assertEquals(7.2, actual, 1e-9)
    }

    @Test
    fun `a cell with no weight row yields the floor of one`() {
        val actual = effectiveStrength(
            strength = 90,
            attrs = abilities(pace = 90, finishing = 90),
            naturalPosition = Position.FORWARD,
            slot = Slot(30),
            context = context(individual = true),
        )
        assertEquals(0.1, actual, 1e-9)
    }

    @Test
    fun `national league handicaps weak and middling clubs`() {
        val weak = effectiveStrength(
            70, abilities(), Position.MIDFIELDER, Slot(12),
            context(kind = CompetitionKind.NATIONAL_LEAGUE, reputation = 2),
        )
        val middling = effectiveStrength(
            70, abilities(), Position.MIDFIELDER, Slot(12),
            context(kind = CompetitionKind.NATIONAL_LEAGUE, reputation = 3),
        )
        val strong = effectiveStrength(
            70, abilities(), Position.MIDFIELDER, Slot(12),
            context(kind = CompetitionKind.NATIONAL_LEAGUE, reputation = 4),
        )
        assertEquals(6.0, weak, 1e-9)
        assertEquals(6.7, middling, 1e-9)
        assertEquals(7.0, strong, 1e-9)
    }

    @Test
    fun `club world cup is the harshest handicap`() {
        val weak = effectiveStrength(
            70, abilities(), Position.MIDFIELDER, Slot(12),
            context(kind = CompetitionKind.CLUB_WORLD_CUP, reputation = 2),
        )
        val outsideEurope = effectiveStrength(
            70, abilities(), Position.MIDFIELDER, Slot(12),
            context(kind = CompetitionKind.CLUB_WORLD_CUP, reputation = 4, continent = 1),
        )
        val european = effectiveStrength(
            70, abilities(), Position.MIDFIELDER, Slot(12),
            context(kind = CompetitionKind.CLUB_WORLD_CUP, reputation = 4, continent = EUROPE_CONTINENT),
        )
        assertEquals(3.9, weak, 1e-9)
        assertEquals(6.3, outsideEurope, 1e-9)
        assertEquals(7.0, european, 1e-9)
    }

    @Test
    fun `the primary continental cup handicaps brazilian clubs specifically`() {
        val brazilian = effectiveStrength(
            70, abilities(), Position.MIDFIELDER, Slot(12),
            context(kind = CompetitionKind.CONTINENTAL_PRIMARY, reputation = 4, country = BRAZIL_COUNTRY),
        )
        val spanish = effectiveStrength(
            70, abilities(), Position.MIDFIELDER, Slot(12),
            context(kind = CompetitionKind.CONTINENTAL_PRIMARY, reputation = 4, country = 65),
        )
        assertEquals(6.3, brazilian, 1e-9)
        assertEquals(7.0, spanish, 1e-9)
    }

    @Test
    fun `national team handicap applies only to players of that country`() {
        val own = effectiveStrength(
            70, abilities(), Position.MIDFIELDER, Slot(12),
            context(kind = CompetitionKind.NATIONAL_TEAM, reputation = 2, represents = true),
        )
        val other = effectiveStrength(
            70, abilities(), Position.MIDFIELDER, Slot(12),
            context(kind = CompetitionKind.NATIONAL_TEAM, reputation = 2, represents = false),
        )
        assertEquals(4.6, own, 1e-9)
        assertEquals(7.0, other, 1e-9)
    }

    @Test
    fun `cup upset handicap weakens only the visiting favourite`() {
        val away = effectiveStrength(
            70, abilities(), Position.MIDFIELDER, Slot(12),
            context(
                kind = CompetitionKind.NATIONAL_CUP,
                isHome = false,
                homeReputation = 2,
                awayReputation = 3,
            ),
        )
        val home = effectiveStrength(
            70, abilities(), Position.MIDFIELDER, Slot(12),
            context(
                kind = CompetitionKind.NATIONAL_CUP,
                isHome = true,
                homeReputation = 2,
                awayReputation = 3,
            ),
        )
        assertEquals(5.6, away, 1e-9)
        assertEquals(7.0, home, 1e-9)
    }

    @Test
    fun `cup upset handicap does not fire when the home side is not an underdog`() {
        val actual = effectiveStrength(
            70, abilities(), Position.MIDFIELDER, Slot(12),
            context(
                kind = CompetitionKind.STATE,
                isHome = false,
                homeReputation = 3,
                awayReputation = 3,
            ),
        )
        assertEquals(7.0, actual, 1e-9)
    }

    @Test
    fun `the out of position penalty is applied before the competition handicap`() {
        val actual = effectiveStrength(
            strength = 71,
            attrs = abilities(),
            naturalPosition = Position.FORWARD,
            slot = Slot(12),
            context = context(kind = CompetitionKind.NATIONAL_LEAGUE, reputation = 2),
        )
        assertEquals(3.1, actual, 1e-9)
    }

    @Test
    fun `every weight row sums to one`() {
        val cells = listOf(1, 2, 3, 8, 9, 10, 11, 13, 14, 16, 17, 18, 19, 24, 25)
        cells.forEach { cell ->
            val total = SlotWeights.forSlot(cell).sum()
            assertTrue(
                kotlin.math.abs(total - 1.0) < 1e-9,
                "weights for slot $cell sum to $total instead of one",
            )
        }
    }

    @Test
    fun `cells outside the pitch have no weight row`() {
        listOf(0, -1, 26, 36).forEach { cell ->
            assertEquals(0.0, SlotWeights.forSlot(cell).sum(), 1e-9, "slot $cell")
        }
    }
}
