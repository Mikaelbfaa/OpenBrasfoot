package org.openbrasfoot.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RuleSetsTest {

    @Test
    fun `modern differs from classic only by the documented deltas`() {
        val rebuilt = RuleSets.MODERN.copy(
            id = RuleSetId.CLASSIC,
            attackSlots = 19..25,
            shotHomeRule = ClassicShotHomeRule,
        )
        assertEquals(RuleSets.CLASSIC, rebuilt)
    }

    @Test
    fun `classic excludes slot eighteen from the attack line`() {
        assertEquals(19..25, RuleSets.CLASSIC.attackSlots)
        assertEquals(18..25, RuleSets.MODERN.attackSlots)
    }

    @Test
    fun `the line divisors are fixed and identical in both rule sets`() {
        listOf(RuleSets.CLASSIC, RuleSets.MODERN).forEach { rules ->
            assertEquals(5.0, rules.defenceDivisor)
            assertEquals(5.0, rules.midfieldDivisor)
            assertEquals(3.0, rules.attackDivisor)
        }
    }

    @Test
    fun `the shooter weight table covers slots zero to twenty five`() {
        val weights = RuleSets.CLASSIC.shooterSlotWeights
        assertEquals(26, weights.size)
        assertEquals(0, weights[0])
        assertEquals(0, weights[1])
        (2..9).forEach { assertEquals(1, weights[it], "slot $it") }
        assertEquals(8, weights[10])
        (11..13).forEach { assertEquals(4, weights[it], "slot $it") }
        (14..17).forEach { assertEquals(8, weights[it], "slot $it") }
        (18..25).forEach { assertEquals(22, weights[it], "slot $it") }
    }

    @Test
    fun `the marking bonus table is indexed by ordinal`() {
        val rules = RuleSets.CLASSIC
        assertEquals(0.0, rules.markingBonus(Marking.LIGHT))
        assertEquals(0.04, rules.markingBonus(Marking.HEAVY))
        assertEquals(0.08, rules.markingBonus(Marking.VERY_HEAVY))
    }

    @Test
    fun `the anti blowout ladder rises with goals already scored`() {
        val ladder = RuleSets.CLASSIC.antiBlowoutLadder
        assertEquals(listOf(3, 5, 6), ladder.map { it.goalsAtLeast })
        assertEquals(4.5, ladder[0].weights.goal)
        assertEquals(3.0, ladder[1].weights.goal)
        assertEquals(0.5, ladder[2].weights.goal)
    }

    @Test
    fun `classic home rule raises both non goal weights for the home side`() {
        val start = ShotMultipliers(1.0, 1.0)
        val home = ClassicShotHomeRule.adjust(start, HomeAdvantage.POSSESSOR_HOME, 0.1)
        assertEquals(1.1, home.saved, 1e-12)
        assertEquals(1.2, home.wide, 1e-12)
    }

    @Test
    fun `classic home rule lowers both non goal weights for the away side`() {
        val start = ShotMultipliers(1.0, 1.0)
        val away = ClassicShotHomeRule.adjust(start, HomeAdvantage.POSSESSOR_AWAY, 0.1)
        assertEquals(0.9, away.saved, 1e-12)
        assertEquals(0.8, away.wide, 1e-12)
    }

    @Test
    fun `classic home rule discards the wide weight it was given`() {
        val withDifferentWide = ShotMultipliers(saved = 1.0, wide = 7.0)
        val home = ClassicShotHomeRule.adjust(withDifferentWide, HomeAdvantage.POSSESSOR_HOME, 0.1)
        assertEquals(1.2, home.wide, 1e-12)
    }

    @Test
    fun `modern home rule keeps the wide weight and favours the home side`() {
        val start = ShotMultipliers(saved = 1.0, wide = 1.25)
        val home = ModernShotHomeRule.adjust(start, HomeAdvantage.POSSESSOR_HOME, 0.1)
        val away = ModernShotHomeRule.adjust(start, HomeAdvantage.POSSESSOR_AWAY, 0.1)
        assertEquals(0.9, home.saved, 1e-12)
        assertEquals(1.15, home.wide, 1e-12)
        assertEquals(1.1, away.saved, 1e-12)
        assertEquals(1.35, away.wide, 1e-12)
    }

    @Test
    fun `neither home rule touches a neutral ground shot`() {
        val start = ShotMultipliers(1.3, 0.7)
        assertSame(start, ClassicShotHomeRule.adjust(start, HomeAdvantage.NONE, 0.1))
        assertSame(start, ModernShotHomeRule.adjust(start, HomeAdvantage.NONE, 0.1))
    }
}
