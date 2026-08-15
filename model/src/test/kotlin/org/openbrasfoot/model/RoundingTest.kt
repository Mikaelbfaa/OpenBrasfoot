package org.openbrasfoot.model

import kotlin.test.Test
import kotlin.test.assertEquals

class RoundingTest {

    @Test
    fun `rounds halfway values up towards positive infinity`() {
        assertEquals(3, bfRound(2.5))
        assertEquals(-2, bfRound(-2.5))
        assertEquals(1, bfRound(0.5))
        assertEquals(0, bfRound(-0.5))
    }

    @Test
    fun `rounds ordinary values to the nearest integer`() {
        assertEquals(2, bfRound(2.4))
        assertEquals(3, bfRound(2.6))
        assertEquals(-3, bfRound(-2.6))
        assertEquals(0, bfRound(0.0))
    }

    @Test
    fun `long variant keeps precision beyond the int range`() {
        assertEquals(7_760_000L, bfRoundLong(7_760_000.4))
        assertEquals(3_000_000_000L, bfRoundLong(2_999_999_999.5))
    }
}
