package org.openbrasfoot.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class RngTest {

    @Test
    fun `same seed produces the same sequence`() {
        val a = SplitMix64Rng(42)
        val b = SplitMix64Rng(42)
        repeat(1000) {
            assertEquals(a.nextBits(), b.nextBits())
        }
    }

    @Test
    fun `different seeds diverge immediately`() {
        assertNotEquals(SplitMix64Rng(42).nextBits(), SplitMix64Rng(43).nextBits())
    }

    @Test
    fun `nextInt stays inside the bound`() {
        val rng = SplitMix64Rng(7)
        repeat(20_000) {
            val value = rng.nextInt(100)
            assertTrue(value in 0..99, "value $value outside 0..99")
        }
    }

    @Test
    fun `nextInt of one is always zero`() {
        val rng = SplitMix64Rng(1)
        repeat(100) {
            assertEquals(0, rng.nextInt(1))
        }
    }

    @Test
    fun `nextInt rejects a non positive bound`() {
        val rng = SplitMix64Rng(1)
        try {
            rng.nextInt(0)
            fail("expected an exception for bound zero")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("positive"))
        }
    }

    @Test
    fun `nextDouble stays in the unit interval`() {
        val rng = SplitMix64Rng(99)
        repeat(20_000) {
            val value = rng.nextDouble()
            assertTrue(value >= 0.0 && value < 1.0, "value $value outside 0 until 1")
        }
    }

    @Test
    fun `nextInt is roughly uniform`() {
        val rng = SplitMix64Rng(0xB2AF00)
        val buckets = IntArray(10)
        val draws = 200_000
        repeat(draws) {
            buckets[rng.nextInt(10)]++
        }
        val expected = draws / 10
        buckets.forEachIndexed { index, count ->
            val drift = kotlin.math.abs(count - expected).toDouble() / expected
            assertTrue(drift < 0.05, "bucket $index drifted $drift from uniform")
        }
    }

    @Test
    fun `fork depends only on origin and tag not on parent position`() {
        val fresh = SplitMix64Rng(2024)
        val advanced = SplitMix64Rng(2024)
        repeat(5000) { advanced.nextBits() }

        val fromFresh = fresh.fork(SeedDomain.MATCH)
        val fromAdvanced = advanced.fork(SeedDomain.MATCH)
        repeat(200) {
            assertEquals(fromFresh.nextBits(), fromAdvanced.nextBits())
        }
    }

    @Test
    fun `different tags give independent children`() {
        val parent = SplitMix64Rng(2024)
        val left = parent.fork(SeedDomain.MATCH)
        val right = parent.fork(SeedDomain.MARKET)
        assertNotEquals(left.nextBits(), right.nextBits())
    }

    @Test
    fun `derived seeds are order sensitive and stable`() {
        val first = deriveSeed(1, SeedDomain.SEASON, 3, 12)
        val second = deriveSeed(1, SeedDomain.SEASON, 3, 12)
        val swapped = deriveSeed(1, SeedDomain.SEASON, 12, 3)
        assertEquals(first, second)
        assertNotEquals(first, swapped)
    }

    @Test
    fun `randRange includes both ends`() {
        val rng = SplitMix64Rng(5)
        var sawLow = false
        var sawHigh = false
        repeat(5000) {
            when (rng.randRange(2, 8)) {
                2 -> sawLow = true
                8 -> sawHigh = true
            }
        }
        assertTrue(sawLow, "never produced the lower bound")
        assertTrue(sawHigh, "never produced the upper bound")
    }

    @Test
    fun `randRange accepts a single value range`() {
        assertEquals(4, SplitMix64Rng(3).randRange(4, 4))
    }

    @Test
    fun `matches the published splitmix64 answer for seed zero`() {
        assertEquals(-0x1DDF57C684E23251L, SplitMix64Rng(0).nextBits())
    }

    @Test
    fun `sequence is pinned so a jdk upgrade cannot change results`() {
        val rng = SplitMix64Rng(42)
        val actual = List(5) { rng.nextBits() }
        val expected = listOf(
            -4767286540954276203L,
            2949826092126892291L,
            5139283748462763858L,
            6349198060258255764L,
            701532786141963250L,
        )
        assertEquals(expected, actual)
    }
}
