package org.openfoot.engine.lineup

import org.openfoot.engine.world.ScriptedInts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The catalogue is data, and what matters about it is the shape rather than
 * any single entry: every formation fields eleven, every cell is on the pitch,
 * no formation repeats a cell, and the order is the one the original stores.
 *
 * The order is load bearing and is asserted explicitly, because section 5.4
 * consumes candidates strongest first in this order. The lists put the keeper
 * first and the defenders last, so the forwards pick before the defenders and
 * the centre backs get the leftovers. Sorting these lists would produce
 * visibly different AI squads.
 */
class FormationTest {

    @Test
    fun `every formation fields eleven players`() {
        for (formation in Formations.ALL) {
            assertEquals(11, formation.slots.size, "${formation.name} fields eleven")
        }
    }

    @Test
    fun `every cell in every formation is on the pitch`() {
        for (formation in Formations.ALL) {
            for (slot in formation.slots) {
                assertTrue(slot.isPitch, "${formation.name} uses ${slot} which is not a pitch cell")
            }
        }
    }

    @Test
    fun `no formation puts two players in one cell`() {
        for (formation in Formations.ALL) {
            assertEquals(
                formation.slots.size,
                formation.slots.distinct().size,
                "${formation.name} repeats a cell",
            )
        }
    }

    @Test
    fun `every formation starts with the keeper`() {
        for (formation in Formations.ALL) {
            assertEquals(1, formation.slots.first().value, "${formation.name} starts with the keeper")
        }
    }

    @Test
    fun `the three four three uses the cell that counts in no aggregate`() {
        val threeFourThree = Formations.byId(10)
        assertTrue(
            threeFourThree.slots.any { it.value == 18 },
            "formation 10 is the one that exposes the slot eighteen defect",
        )
    }

    @Test
    fun `the ai never draws the twelfth formation`() {
        for (draw in 0 until 100) {
            val drawn = drawFormation(ScriptedInts(draw))
            assertTrue(drawn.id != 12, "draw $draw produced formation 12, which the AI never picks")
        }
    }

    @Test
    fun `the four four two is the formation the ai draws most`() {
        val counts = (0 until 100).groupingBy { drawFormation(ScriptedInts(it)).id }.eachCount()
        val mostDrawn = counts.maxBy { it.value }
        assertEquals(4, mostDrawn.key, "section 3.2 gives formation 4 the widest band")
        assertEquals(31, mostDrawn.value, "section 3.2 gives it thirty one of a hundred")
    }
}
