package org.openfoot.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The one shape every draw table in section 3.8 is written in: an ordered list
 * of ranges, first match wins.
 */
class BandTest {

    private val table = listOf(Band(0..0, "keeper"), Band(1..79, "midfield"), Band(80..199, "rest"))

    @Test
    fun `the first band whose range contains the draw wins`() {
        assertEquals("keeper", table.pick(0))
        assertEquals("midfield", table.pick(1))
        assertEquals("midfield", table.pick(79))
        assertEquals("rest", table.pick(80))
        assertEquals("rest", table.pick(199))
    }

    @Test
    fun `the bound is one past the last band`() {
        assertEquals(200, table.bound())
    }

    @Test
    fun `a draw outside every band is a programming error, not a default`() {
        assertFailsWith<NoSuchElementException> { table.pick(200) }
    }
}
