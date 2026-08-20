package org.openfoot.engine.lineup

import org.openfoot.engine.world.ScriptedInts
import org.openfoot.model.SlotGroup
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

    /**
     * Section 5.1's table, re-read from the spec independently of
     * Formation.kt rather than copied out of it, so this comparison cannot
     * simply agree with itself. Cross checked twice already by two separate
     * readings during review; this test's job from here on is to be a
     * tripwire that stops that transcription drifting silently, not to prove
     * it right a third time, in the same spirit as MatchGoldenVectorTest.
     */
    @Test
    fun `the twelve formations match section 5 point 1 exactly, cell for cell and in order`() {
        val expected = mapOf(
            1 to listOf(1, 20, 11, 13, 14, 16, 2, 9, 6, 4, 8),
            2 to listOf(1, 22, 24, 12, 14, 16, 2, 9, 6, 4, 8),
            3 to listOf(1, 23, 11, 13, 15, 2, 9, 6, 8, 10, 17),
            4 to listOf(1, 22, 24, 11, 13, 14, 16, 2, 9, 3, 5),
            5 to listOf(1, 19, 21, 11, 12, 13, 15, 2, 9, 6, 8),
            6 to listOf(1, 22, 24, 12, 14, 15, 16, 2, 9, 6, 8),
            7 to listOf(1, 22, 23, 24, 12, 14, 16, 2, 9, 6, 8),
            8 to listOf(1, 19, 20, 21, 11, 13, 15, 2, 9, 6, 8),
            9 to listOf(1, 22, 24, 11, 13, 15, 4, 6, 8, 10, 17),
            10 to listOf(1, 18, 25, 23, 11, 13, 4, 6, 8, 10, 17),
            11 to listOf(1, 23, 14, 16, 15, 13, 11, 2, 9, 6, 8),
            12 to listOf(1, 20, 10, 17, 15, 13, 11, 2, 9, 6, 8),
        )
        for ((id, slots) in expected) {
            val formation = Formations.byId(id)
            assertEquals(
                slots,
                formation.slots.map { it.value },
                "formation $id (${formation.name}) no longer matches section 5.1",
            )
        }
    }

    /**
     * A pure reordering of a formation's own eleven cells leaves the eleven
     * players fielded unchanged, so the golden vectors and every shape test
     * above are blind to it. This pins the one ordering property that section
     * 5.4's automatic lineup actually depends on: it fills slots in list
     * order from a pool sorted strongest first, so a defence cell ahead of an
     * attack cell would seat a weaker player at the back before a stronger
     * one gets tried up front.
     *
     * Checked by hand against every row of section 5.1's table before writing
     * this assertion: in each of the twelve formations the keeper is first
     * and every defence cell (slot 2-9) comes after every attack cell (slot
     * 19-25). Formation 10's slot 18 belongs to neither group and is skipped.
     */
    @Test
    fun `every formation seats the keeper first and no defender ahead of a forward`() {
        for (formation in Formations.ALL) {
            assertEquals(1, formation.slots.first().value, "${formation.name} does not start with the keeper")

            var sawDefence = false
            for (slot in formation.slots.drop(1)) {
                when (slot.group) {
                    SlotGroup.DEFENCE -> sawDefence = true
                    SlotGroup.ATTACK -> assertTrue(
                        !sawDefence,
                        "${formation.name} places forward $slot behind a defender",
                    )
                    else -> Unit
                }
            }
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

    /**
     * Section 3.2's rand(1..100) table, band by band, re-read from the spec
     * rather than copied out of Formation.kt so that this comparison cannot
     * simply agree with itself. The spec writes it as one line: 1-2 gives
     * formation 1, 3-4 formation 2, 5-7 formation 3, 8-38 formation 4, 39-49
     * formation 5, 50-60 formation 6, 61-65 formation 7, 66-72 formation 8,
     * 73-90 formation 9, 91-92 formation 10 and 93-100 formation 11.
     *
     * The two tests below it assert only that formation 12 is never drawn and
     * that formation 4 has the widest band, so every other band could be
     * remapped without a test noticing. That matters beyond the draw itself:
     * item 32's headline figure weights each formation by the width of its
     * band, so a wrong band silently moves a published number.
     *
     * This test's job is to be a change detector, the same one the exact
     * formation table above has: the shape tests prove the catalogue is
     * consistent, and this proves the transcription still matches the spec.
     * The draw is one based and the engine's rand is nought based, so a draw
     * of n comes from a scripted rand of n minus one.
     */
    @Test
    fun `every band of the section 3 point 2 draw table maps to the formation the spec gives it`() {
        val bands = listOf(
            1..2 to 1,
            3..4 to 2,
            5..7 to 3,
            8..38 to 4,
            39..49 to 5,
            50..60 to 6,
            61..65 to 7,
            66..72 to 8,
            73..90 to 9,
            91..92 to 10,
            93..100 to 11,
        )

        assertEquals(
            (1..100).toList(),
            bands.flatMap { (band, _) -> band.toList() },
            "the bands must tile one to a hundred exactly once, in order",
        )

        for ((band, id) in bands) {
            for (draw in band) {
                val drawn = drawFormation(ScriptedInts(draw - 1))
                assertEquals(id, drawn.id, "draw $draw should give formation $id, gave ${drawn.name}")
            }
        }

        val widths = bands.associate { (band, id) -> id to (band.last - band.first + 1) }
        assertEquals(
            mapOf(1 to 2, 2 to 2, 3 to 3, 4 to 31, 5 to 11, 6 to 11, 7 to 5, 8 to 7, 9 to 18, 10 to 2, 11 to 8),
            widths,
            "these widths are the weights item 32 uses to average over formations",
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
