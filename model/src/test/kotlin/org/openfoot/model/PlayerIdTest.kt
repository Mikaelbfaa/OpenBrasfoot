package org.openfoot.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PlayerIdTest {

    @Test
    fun `two identities with the same number are the same identity`() {
        assertEquals(PlayerId(4), PlayerId(4))
    }

    @Test
    fun `two identities with different numbers differ`() {
        assertNotEquals(PlayerId(4), PlayerId(5))
    }

    @Test
    fun `the unassigned identity is outside any squad index`() {
        assertEquals(-1, PlayerId.UNASSIGNED.value)
    }
}
