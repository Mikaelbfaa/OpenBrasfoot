package org.openfoot.engine.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The world creation row of the section 4.7 table.
 */
class PlayerTermsTest {

    @Test
    fun `a contract runs two hundred and ten days plus the draw`() {
        assertEquals(210, initialContractDays(ScriptedInts(0)))
        assertEquals(225, initialContractDays(ScriptedInts(15)))
        assertEquals(239, initialContractDays(ScriptedInts(29)))
    }

    @Test
    fun `the spread never reaches a full extra month`() {
        val rng = ScriptedInts(29)
        assertTrue(initialContractDays(rng) < 240)
    }

    @Test
    fun `one contract costs exactly one draw`() {
        val rng = ScriptedInts(7)
        initialContractDays(rng)
        assertEquals(1, rng.draws)
    }
}
