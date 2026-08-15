package org.openbrasfoot.engine.match

import org.openbrasfoot.model.RuleSets
import kotlin.test.Test
import kotlin.test.assertEquals

class StrengthDifferenceTest {

    private val rules = RuleSets.CLASSIC

    @Test
    fun `the difference is signed and scaled`() {
        assertEquals(0.125, strengthDifference(6.0, 5.0, 8.0), 1e-12)
        assertEquals(-0.125, strengthDifference(5.0, 6.0, 8.0), 1e-12)
        assertEquals(0.0, strengthDifference(5.0, 5.0, 8.0), 1e-12)
    }

    @Test
    fun `every duel shares the same divisor before the compression season`() {
        listOf(1, 2, 3, 4).forEach { season ->
            DuelKind.entries.forEach { duel ->
                assertEquals(8.0, differenceDivisor(season, duel, rules), "season $season, $duel")
            }
        }
    }

    @Test
    fun `from the compression season the duels diverge`() {
        assertEquals(11.0, differenceDivisor(5, DuelKind.POSSESSION, rules))
        assertEquals(11.0, differenceDivisor(5, DuelKind.CHANCE, rules))
        assertEquals(10.0, differenceDivisor(5, DuelKind.SHOT, rules))
    }

    @Test
    fun `the compressed divisors hold for every later season`() {
        listOf(6, 9, 20, 100).forEach { season ->
            assertEquals(11.0, differenceDivisor(season, DuelKind.POSSESSION, rules), "season $season")
            assertEquals(10.0, differenceDivisor(season, DuelKind.SHOT, rules), "season $season")
        }
    }

    @Test
    fun `compression makes the same gap worth less`() {
        val early = strengthDifference(7.0, 5.0, differenceDivisor(1, DuelKind.POSSESSION, rules))
        val late = strengthDifference(7.0, 5.0, differenceDivisor(5, DuelKind.POSSESSION, rules))
        assertEquals(0.25, early, 1e-12)
        assertEquals(2.0 / 11.0, late, 1e-12)
    }
}
