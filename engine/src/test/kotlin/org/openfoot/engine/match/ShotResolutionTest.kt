package org.openfoot.engine.match

import org.openfoot.model.HomeAdvantage
import org.openfoot.model.RuleSet
import org.openfoot.model.RuleSets
import org.openfoot.model.SplitMix64Rng
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Threshold tests for spec section 3.6c. Each expected boundary is written as
 * the fraction it comes from, so the arithmetic can be checked on paper.
 */
class ShotResolutionTest {

    private val classic = RuleSets.CLASSIC
    private val modern = RuleSets.MODERN

    private fun inputs(
        shooter: Double = 5.0,
        keeper: Double = 5.0,
        attack: Double = 5.0,
        defence: Double = 5.0,
        goals: Int = 0,
        reputationGap: Int = 0,
        advantage: HomeAdvantage = HomeAdvantage.NONE,
        humanInvolved: Boolean = false,
        centrebacks: Int = 4,
        divisor: Double = 8.0,
    ) = ShotInputs(
        shooterRating = shooter,
        keeperRating = keeper,
        possessorAttack = attack,
        defenderDefence = defence,
        goalsAlreadyScored = goals,
        reputationGap = reputationGap,
        advantage = advantage,
        humanInvolved = humanInvolved,
        defenderCentrebacks = centrebacks,
        divisor = divisor,
    )

    private fun outcome(inputs: ShotInputs, draw: Double, rules: RuleSet = classic) =
        shotOutcome(inputs, rules, ScriptedRng(draw))

    private fun assertGoalBoundary(
        inputs: ShotInputs,
        below: Double,
        above: Double,
        rules: RuleSet = classic,
        hint: String = "",
    ) {
        assertEquals(ShotOutcome.GOAL, outcome(inputs, below, rules), "below the boundary $hint")
        assertTrue(
            outcome(inputs, above, rules) != ShotOutcome.GOAL,
            "above the boundary $hint",
        )
    }

    @Test
    fun `on neutral ground about one shot in ten is a goal`() {
        assertGoalBoundary(inputs(), below = 0.0981, above = 0.0982, hint = "5.5 over 56.05")
    }

    @Test
    fun `the home side converts worse under classic`() {
        assertGoalBoundary(
            inputs(advantage = HomeAdvantage.POSSESSOR_HOME),
            below = 0.0878,
            above = 0.0879,
            hint = "5.5 over 62.605",
        )
    }

    @Test
    fun `the visiting side converts better under classic`() {
        assertGoalBoundary(
            inputs(advantage = HomeAdvantage.POSSESSOR_AWAY),
            below = 0.1111,
            above = 0.1112,
            hint = "5.5 over 49.495",
        )
    }

    @Test
    fun `modern reverses the home bias`() {
        assertGoalBoundary(
            inputs(advantage = HomeAdvantage.POSSESSOR_HOME),
            below = 0.1078,
            above = 0.1079,
            rules = modern,
            hint = "5.5 over 50.995",
        )
        assertGoalBoundary(
            inputs(advantage = HomeAdvantage.POSSESSOR_AWAY),
            below = 0.0900,
            above = 0.0901,
            rules = modern,
            hint = "5.5 over 61.105",
        )
    }

    @Test
    fun `a better keeper suppresses conversion`() {
        assertGoalBoundary(
            inputs(keeper = 7.0, shooter = 5.0),
            below = 0.0846,
            above = 0.0847,
            hint = "5.5 over 64.9375",
        )
    }

    @Test
    fun `the anti blowout ladder bites as goals pile up`() {
        assertGoalBoundary(inputs(goals = 3), 0.0749, 0.0750, hint = "4.5 over 60.05")
        assertGoalBoundary(inputs(goals = 5), 0.0512, 0.0513, hint = "3.0 over 58.55")
        assertGoalBoundary(inputs(goals = 6), 0.0089, 0.0090, hint = "0.5 over 56.05")
    }

    @Test
    fun `the ladder only steps at its thresholds`() {
        listOf(0, 1, 2).forEach { goals ->
            assertEquals(classic.shotBaseWeightsFor(goals), classic.shotBaseWeights, "goals $goals")
        }
        assertEquals(4.5, shotBaseWeights(4, 0, classic).goal)
        assertEquals(3.0, shotBaseWeights(5, 0, classic).goal)
        assertEquals(0.5, shotBaseWeights(7, 0, classic).goal)
    }

    @Test
    fun `an outclassed side that is two up is pulled back to the middle rung`() {
        assertGoalBoundary(
            inputs(goals = 2, reputationGap = 2),
            below = 0.0512,
            above = 0.0513,
            hint = "3.0 over 58.55",
        )
    }

    @Test
    fun `the outclassed override is applied after the ladder`() {
        assertEquals(0.5, shotBaseWeights(6, 0, classic).goal)
        assertEquals(3.0, shotBaseWeights(6, 2, classic).goal)
        assertGoalBoundary(inputs(goals = 6, reputationGap = 2), 0.0512, 0.0513)
    }

    @Test
    fun `a favourite gets no override`() {
        assertEquals(0.5, shotBaseWeights(6, -2, classic).goal)
    }

    @Test
    fun `a human defence without centre backs is stripped of its keeper`() {
        assertGoalBoundary(
            inputs(humanInvolved = true, centrebacks = 0),
            below = 0.1992,
            above = 0.1993,
            hint = "5.5 over 27.61",
        )
    }

    @Test
    fun `with an average keeper zero and one centre back collapse the same way`() {
        listOf(0, 1).forEach { count ->
            assertGoalBoundary(
                inputs(humanInvolved = true, centrebacks = count),
                below = 0.1992,
                above = 0.1993,
                hint = "centrebacks $count",
            )
        }
    }

    @Test
    fun `with a strong keeper the two anti exploit factors do diverge`() {
        assertGoalBoundary(
            inputs(keeper = 7.0, humanInvolved = true, centrebacks = 1),
            below = 0.0981,
            above = 0.0982,
            hint = "rounds back up to one",
        )
        assertGoalBoundary(
            inputs(keeper = 7.0, humanInvolved = true, centrebacks = 0),
            below = 0.1992,
            above = 0.1993,
            hint = "rounds to zero then hits the floor",
        )
    }

    @Test
    fun `an artificial defence is never stripped`() {
        assertGoalBoundary(
            inputs(humanInvolved = false, centrebacks = 0),
            below = 0.0981,
            above = 0.0982,
        )
    }

    @Test
    fun `classic throws away the defence comparison whenever there is home advantage`() {
        val rates = (3..9).map { goalRate(defence = it.toDouble(), rules = classic) }
        assertEquals(1, rates.distinct().size, "defence changed the outcome, measured $rates")
    }

    @Test
    fun `modern keeps the defence comparison`() {
        val rates = (3..9).map { goalRate(defence = it.toDouble(), rules = modern) }
        rates.zipWithNext().forEach { (stronger, weaker) ->
            assertTrue(weaker < stronger, "a better defence should concede fewer goals, got $rates")
        }
    }

    @Test
    fun `classic still reads the defence on neutral ground`() {
        val rates = (3..9).map {
            goalRate(defence = it.toDouble(), rules = classic, advantage = HomeAdvantage.NONE)
        }
        assertTrue(rates.distinct().size > 1, "defence should matter on neutral ground, got $rates")
    }

    @Test
    fun `the empirical conversion rate matches the documented one`() {
        val rng = SplitMix64Rng(0x5407)
        val draws = 200_000
        val goals = (1..draws).count { shotOutcome(inputs(), classic, rng) == ShotOutcome.GOAL }
        val share = goals.toDouble() / draws
        assertTrue(abs(share - 5.5 / 56.05) < 0.004, "expected about 0.0981, measured $share")
    }

    private fun goalRate(
        defence: Double,
        rules: RuleSet,
        advantage: HomeAdvantage = HomeAdvantage.POSSESSOR_HOME,
    ): Double {
        val rng = SplitMix64Rng(4242)
        val draws = 20_000
        val goals = (1..draws).count {
            shotOutcome(inputs(defence = defence, advantage = advantage), rules, rng) ==
                ShotOutcome.GOAL
        }
        return goals.toDouble() / draws
    }

    private fun RuleSet.shotBaseWeightsFor(goals: Int) = shotBaseWeights(goals, 0, this)
}
