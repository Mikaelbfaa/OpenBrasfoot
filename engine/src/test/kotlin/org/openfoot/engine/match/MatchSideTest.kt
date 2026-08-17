package org.openfoot.engine.match

import org.openfoot.model.CompetitionKind
import org.openfoot.model.Marking
import org.openfoot.model.Position
import org.openfoot.model.RuleSets
import org.openfoot.model.TeamSide
import org.openfoot.model.HomeAdvantage
import org.openfoot.model.Trait
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MatchSideTest {

    @Test
    fun `rating delegates to effective strength`() {
        val side = Lineups.sideOfSlots(listOf(12), strength = 70)
        assertEquals(7.0, side.ratingOf(side.lineup[0]), 1e-9)
    }

    @Test
    fun `rating applies the out of position penalty`() {
        val striker = Lineups.player(slot = 12, strength = 70, position = Position.FORWARD)
        val side = Lineups.side(listOf(striker))
        assertEquals(3.5, side.ratingOf(striker), 1e-9)
    }

    @Test
    fun `the national team handicap is plumbed per player`() {
        val context = Lineups.context(kind = CompetitionKind.NATIONAL_TEAM, reputation = 2)
        val countryman = Lineups.player(slot = 12, strength = 70, representsSideCountry = true)
        val foreigner = Lineups.player(slot = 13, strength = 70)
        val side = Lineups.side(listOf(countryman, foreigner), context = context)

        assertEquals(4.6, side.ratingOf(countryman), 1e-9)
        assertEquals(7.0, side.ratingOf(foreigner), 1e-9)
    }

    @Test
    fun `reputation and home flag come from the shared context`() {
        val side = Lineups.side(
            listOf(Lineups.player(12, 50)),
            context = Lineups.context(reputation = 3, isHome = false),
        )
        assertEquals(3, side.reputation)
        assertFalse(side.isHome)
    }

    @Test
    fun `a player carries both of his characteristics`() {
        val player = Lineups.player(
            slot = 20,
            strength = 50,
            firstTrait = Trait.FINISHING,
            secondTrait = Trait.PACE,
        )
        assertTrue(player.hasTrait(Trait.FINISHING))
        assertTrue(player.hasTrait(Trait.PACE))
        assertFalse(player.hasTrait(Trait.TACKLING))
    }

    @Test
    fun `the setup reports a human side when either club is managed`() {
        val plain = Lineups.side(listOf(Lineups.player(12, 50)))
        val managed = Lineups.side(listOf(Lineups.player(12, 50)), humanManaged = true)

        assertFalse(setup(plain, plain).hasHumanSide)
        assertTrue(setup(managed, plain).hasHumanSide)
        assertTrue(setup(plain, managed).hasHumanSide)
    }

    @Test
    fun `home advantage follows the possessor on ordinary ground`() {
        val side = Lineups.side(listOf(Lineups.player(12, 50)))
        val setup = setup(side, side)
        assertEquals(HomeAdvantage.POSSESSOR_HOME, setup.advantageFor(TeamSide.HOME))
        assertEquals(HomeAdvantage.POSSESSOR_AWAY, setup.advantageFor(TeamSide.AWAY))
    }

    @Test
    fun `home advantage disappears in the two neutral ground competitions`() {
        listOf(CompetitionKind.CLUB_WORLD_CUP, CompetitionKind.NATIONAL_TEAM).forEach { kind ->
            val side = Lineups.side(
                listOf(Lineups.player(12, 50)),
                context = Lineups.context(kind = kind),
            )
            val setup = setup(side, side)
            assertTrue(setup.isNeutralGround, "$kind should be neutral ground")
            assertEquals(HomeAdvantage.NONE, setup.advantageFor(TeamSide.HOME))
            assertEquals(HomeAdvantage.NONE, setup.advantageFor(TeamSide.AWAY))
        }
    }

    @Test
    fun `the setup resolves a team side to its lineup`() {
        val home = Lineups.side(listOf(Lineups.player(12, 60)), marking = Marking.HEAVY)
        val away = Lineups.side(listOf(Lineups.player(12, 40)))
        val setup = setup(home, away)
        assertEquals(Marking.HEAVY, setup.side(TeamSide.HOME).marking)
        assertEquals(40, setup.side(TeamSide.AWAY).lineup[0].strength)
    }

    private fun setup(home: MatchSide, away: MatchSide) =
        MatchSetup(home = home, away = away, season = 1, rules = RuleSets.CLASSIC)
}
