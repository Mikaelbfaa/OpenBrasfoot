package org.openbrasfoot.engine.match

import org.openbrasfoot.model.CompetitionKind
import org.openbrasfoot.model.HomeAdvantage
import org.openbrasfoot.model.RuleSets
import org.openbrasfoot.model.SplitMix64Rng
import org.openbrasfoot.model.TeamSide
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Threshold tests for spec sections 3.6a and 3.6b.
 *
 * Each documented probability is pinned by feeding a draw just below and just
 * above the boundary, so there is no sampling error. The expected boundaries
 * are written out as the fraction they come from.
 */
class DuelsTest {

    private val rules = RuleSets.CLASSIC

    private fun possession(
        possessorMidfield: Double,
        defenderMidfield: Double,
        advantage: HomeAdvantage,
        divisor: Double,
        draw: Double,
    ) = possessionDuel(
        possessorMidfield, defenderMidfield, advantage, divisor, rules, ScriptedRng(draw),
    )

    private fun chance(
        attack: Double,
        defence: Double,
        advantage: HomeAdvantage = HomeAdvantage.NONE,
        humanInvolved: Boolean = false,
        centrebacks: Int = 4,
        divisor: Double = 8.0,
        draw: Double,
    ) = chanceDuel(
        attack, defence, advantage, humanInvolved, centrebacks, divisor, rules, ScriptedRng(draw),
    )

    @Test
    fun `at parity the side on the ball keeps it fifty five percent of the time`() {
        assertEquals(
            DuelWinner.POSSESSOR,
            possession(5.0, 5.0, HomeAdvantage.NONE, 8.0, draw = 0.5499),
        )
        assertEquals(
            DuelWinner.DEFENDER,
            possession(5.0, 5.0, HomeAdvantage.NONE, 8.0, draw = 0.5501),
        )
    }

    @Test
    fun `at parity a home possessor keeps it about sixty one percent of the time`() {
        assertEquals(
            DuelWinner.POSSESSOR,
            possession(5.0, 5.0, HomeAdvantage.POSSESSOR_HOME, 8.0, draw = 0.6137),
        )
        assertEquals(
            DuelWinner.DEFENDER,
            possession(5.0, 5.0, HomeAdvantage.POSSESSOR_HOME, 8.0, draw = 0.6138),
        )
    }

    @Test
    fun `the visiting possessor gets no bonus`() {
        assertEquals(
            DuelWinner.POSSESSOR,
            possession(5.0, 5.0, HomeAdvantage.POSSESSOR_AWAY, 8.0, draw = 0.5499),
        )
        assertEquals(
            DuelWinner.DEFENDER,
            possession(5.0, 5.0, HomeAdvantage.POSSESSOR_AWAY, 8.0, draw = 0.5501),
        )
    }

    @Test
    fun `a two point midfield edge is worth sixty seven percent early on`() {
        assertEquals(
            DuelWinner.POSSESSOR,
            possession(7.0, 5.0, HomeAdvantage.NONE, 8.0, draw = 0.6707),
        )
        assertEquals(
            DuelWinner.DEFENDER,
            possession(7.0, 5.0, HomeAdvantage.NONE, 8.0, draw = 0.6708),
        )
    }

    @Test
    fun `the same edge is worth less once the divisors compress`() {
        assertEquals(
            DuelWinner.POSSESSOR,
            possession(7.0, 5.0, HomeAdvantage.NONE, 11.0, draw = 0.6383),
        )
        assertEquals(
            DuelWinner.DEFENDER,
            possession(7.0, 5.0, HomeAdvantage.NONE, 11.0, draw = 0.6384),
        )
    }

    @Test
    fun `the possession split is empirically what the weights say`() {
        val rng = SplitMix64Rng(0xB2AF00)
        val draws = 100_000
        val kept = (1..draws).count {
            possessionDuel(5.0, 5.0, HomeAdvantage.POSSESSOR_HOME, 8.0, rules, rng) ==
                DuelWinner.POSSESSOR
        }
        val share = kept.toDouble() / draws
        assertTrue(abs(share - 0.613734) < 0.006, "expected about 0.6137, measured $share")
    }

    @Test
    fun `at parity a chance is created half the time`() {
        assertEquals(ChanceOutcome.SHOT, chance(5.0, 5.0, draw = 0.4999))
        assertEquals(ChanceOutcome.NO_SHOT, chance(5.0, 5.0, draw = 0.5001))
    }

    @Test
    fun `at parity a home possessor creates a chance about fifty seven percent of the time`() {
        assertEquals(
            ChanceOutcome.SHOT,
            chance(5.0, 5.0, advantage = HomeAdvantage.POSSESSOR_HOME, draw = 0.5652),
        )
        assertEquals(
            ChanceOutcome.NO_SHOT,
            chance(5.0, 5.0, advantage = HomeAdvantage.POSSESSOR_HOME, draw = 0.5653),
        )
    }

    @Test
    fun `an empty attack still creates the occasional chance`() {
        assertEquals(ChanceOutcome.SHOT, chance(0.0, 5.0, draw = 0.1095))
        assertEquals(ChanceOutcome.NO_SHOT, chance(0.0, 5.0, draw = 0.1097))
    }

    @Test
    fun `an empty defence concedes almost every chance`() {
        assertEquals(ChanceOutcome.SHOT, chance(5.0, 0.0, draw = 0.8904))
        assertEquals(ChanceOutcome.NO_SHOT, chance(5.0, 0.0, draw = 0.8905))
    }

    @Test
    fun `a human side without centre backs is punished`() {
        assertEquals(
            ChanceOutcome.SHOT,
            chance(5.0, 5.0, humanInvolved = true, centrebacks = 0, draw = 0.8333),
        )
        assertEquals(
            ChanceOutcome.NO_SHOT,
            chance(5.0, 5.0, humanInvolved = true, centrebacks = 0, draw = 0.8334),
        )
    }

    @Test
    fun `zero and one centre backs are indistinguishable because the floor catches both`() {
        listOf(0, 1).forEach { count ->
            assertEquals(
                ChanceOutcome.SHOT,
                chance(5.0, 5.0, humanInvolved = true, centrebacks = count, draw = 0.8333),
                "centrebacks $count",
            )
            assertEquals(
                ChanceOutcome.NO_SHOT,
                chance(5.0, 5.0, humanInvolved = true, centrebacks = count, draw = 0.8334),
                "centrebacks $count",
            )
        }
    }

    @Test
    fun `an artificial side is never punished for a broken back line`() {
        assertEquals(
            ChanceOutcome.SHOT,
            chance(5.0, 5.0, humanInvolved = false, centrebacks = 0, draw = 0.4999),
        )
        assertEquals(
            ChanceOutcome.NO_SHOT,
            chance(5.0, 5.0, humanInvolved = false, centrebacks = 0, draw = 0.5001),
        )
    }

    @Test
    fun `centre backs are counted by cell not by natural position`() {
        val side = Lineups.sideOfSlots(Lineups.FORMATION_4_4_2, strength = 50)
        assertEquals(2, centrebackCount(side, rules))

        val backThree = Lineups.sideOfSlots(listOf(4, 6, 8, 2, 9), strength = 50)
        assertEquals(3, centrebackCount(backThree, rules))
    }

    @Test
    fun `the pairing facade resolves aggregates and returns the winning side`() {
        val side = Lineups.sideOfSlots(Lineups.FORMATION_4_4_2, strength = 60)
        val setup = MatchSetup(home = side, away = side, season = 1, rules = rules)
        val rng = SplitMix64Rng(7)
        val draws = 20_000
        val kept = (1..draws).count {
            possessionDuel(setup, TeamSide.HOME, rng) == TeamSide.HOME
        }
        val share = kept.toDouble() / draws
        assertTrue(abs(share - 0.613734) < 0.015, "expected about 0.6137, measured $share")
    }

    @Test
    fun `neutral ground removes the home edge from the facade`() {
        val side = Lineups.sideOfSlots(
            Lineups.FORMATION_4_4_2,
            strength = 60,
            context = Lineups.context(kind = CompetitionKind.CLUB_WORLD_CUP),
        )
        val setup = MatchSetup(home = side, away = side, season = 1, rules = rules)
        val rng = SplitMix64Rng(11)
        val draws = 20_000
        val kept = (1..draws).count {
            possessionDuel(setup, TeamSide.HOME, rng) == TeamSide.HOME
        }
        val share = kept.toDouble() / draws
        assertTrue(abs(share - 0.55) < 0.015, "expected about 0.55, measured $share")
    }
}
