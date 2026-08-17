package org.openfoot.engine.match

import org.openfoot.model.SplitMix64Rng
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeightedChoiceTest {

    @Test
    fun `a draw below the first boundary selects the first outcome`() {
        val rng = ScriptedRng(0.49)
        assertEquals(0, weightedPick(doubleArrayOf(1.0, 1.0), doubleArrayOf(1.0, 1.0), rng))
    }

    @Test
    fun `a draw above the first boundary selects the second outcome`() {
        val rng = ScriptedRng(0.51)
        assertEquals(1, weightedPick(doubleArrayOf(1.0, 1.0), doubleArrayOf(1.0, 1.0), rng))
    }

    @Test
    fun `a draw exactly on a boundary selects the following outcome`() {
        val rng = ScriptedRng(0.5)
        assertEquals(1, weightedPick(doubleArrayOf(1.0, 1.0), doubleArrayOf(1.0, 1.0), rng))
    }

    @Test
    fun `multipliers move the boundary`() {
        val base = doubleArrayOf(55.0, 45.0)
        val multipliers = doubleArrayOf(1.3, 1.0)
        assertEquals(0, weightedPick(base, multipliers, ScriptedRng(0.6137)))
        assertEquals(1, weightedPick(base, multipliers, ScriptedRng(0.6138)))
    }

    @Test
    fun `exactly one draw is consumed per call`() {
        val rng = ScriptedRng(0.1, 0.9)
        weightedPick(doubleArrayOf(1.0, 1.0), doubleArrayOf(1.0, 1.0), rng)
        assertEquals(1, rng.draws)
        weightedPick(doubleArrayOf(1.0, 1.0), doubleArrayOf(1.0, 1.0), rng)
        assertEquals(2, rng.draws)
    }

    @Test
    fun `a zero total returns the last outcome without throwing`() {
        val rng = ScriptedRng(0.7)
        assertEquals(2, weightedPick(doubleArrayOf(0.0, 0.0, 0.0), doubleArrayOf(1.0, 1.0, 1.0), rng))
        assertEquals(1, rng.draws)
    }

    @Test
    fun `a single outcome is always chosen`() {
        assertEquals(0, weightedPick(doubleArrayOf(4.0), doubleArrayOf(1.0), ScriptedRng(0.99)))
    }

    @Test
    fun `three outcomes split at the right places`() {
        val weights = doubleArrayOf(5.5, 35.55, 15.0)
        assertEquals(0, weightedPick(weights, ScriptedRng(0.0980)))
        assertEquals(1, weightedPick(weights, ScriptedRng(0.0983)))
        assertEquals(1, weightedPick(weights, ScriptedRng(0.7323)))
        assertEquals(2, weightedPick(weights, ScriptedRng(0.7325)))
    }

    @Test
    fun `the empirical split matches the weights`() {
        val rng = SplitMix64Rng(1)
        val weights = doubleArrayOf(3.0, 1.0)
        var first = 0
        val draws = 100_000
        repeat(draws) {
            if (weightedPick(weights, rng) == 0) {
                first++
            }
        }
        val share = first.toDouble() / draws
        assertTrue(abs(share - 0.75) < 0.006, "expected about 0.75, measured $share")
    }

    @Test
    fun `the empirical split is reproducible from the same seed`() {
        fun run(): Int {
            val rng = SplitMix64Rng(99)
            return (1..10_000).count { weightedPick(doubleArrayOf(2.0, 1.0), rng) == 0 }
        }
        assertEquals(run(), run())
    }

    @Test
    fun `a mismatched multiplier array is rejected`() {
        val error = runCatching {
            weightedPick(doubleArrayOf(1.0, 1.0), doubleArrayOf(1.0), ScriptedRng(0.5))
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException, "expected an argument error, got $error")
    }
}
