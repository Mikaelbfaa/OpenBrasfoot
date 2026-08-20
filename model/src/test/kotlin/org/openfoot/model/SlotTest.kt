package org.openfoot.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SlotTest {

    @Test
    fun `pitch slots map to the position they ask for`() {
        assertEquals(Position.GOALKEEPER, Slot(1).requiredPosition)
        assertEquals(Position.FULLBACK, Slot(2).requiredPosition)
        assertEquals(Position.FULLBACK, Slot(9).requiredPosition)
        assertEquals(Position.CENTREBACK, Slot(3).requiredPosition)
        assertEquals(Position.CENTREBACK, Slot(8).requiredPosition)
        assertEquals(Position.MIDFIELDER, Slot(11).requiredPosition)
        assertEquals(Position.MIDFIELDER, Slot(16).requiredPosition)
        assertEquals(Position.FORWARD, Slot(19).requiredPosition)
        assertEquals(Position.FORWARD, Slot(25).requiredPosition)
    }

    @Test
    fun `wing back cells ask for a fullback`() {
        assertEquals(Position.FULLBACK, Slot(10).requiredPosition)
        assertEquals(Position.FULLBACK, Slot(17).requiredPosition)
    }

    @Test
    fun `off pitch cells ask for nothing`() {
        assertNull(Slot(0).requiredPosition)
        assertNull(Slot(-1).requiredPosition)
        assertNull(Slot(26).requiredPosition)
        assertNull(Slot(36).requiredPosition)
    }

    /**
     * The side column of the section 3.2 table, re-read from the spec rather
     * than copied out of Slot.kt, over every cell of the grid.
     *
     * Section 3.2 gives a side to three pairs of cells and to nothing else:
     * the laterais 2 and 9, written "direito / esquerdo", the alas 10 and 17
     * and the pontas 18 and 25. The lower number of each pair is the right,
     * following the order the fullback row spells out. The other nineteen
     * pitch cells are central and take either side.
     *
     * This is a change detector, in the same spirit as the exact formation
     * table in FormationTest. The lineup tests in AutoLineupTest prove that
     * the table is read and obeyed; this one proves it still says what section
     * 3.2 says.
     */
    @Test
    fun `only the three flank pairs of section 3 point 2 demand a side`() {
        val right = setOf(2, 10, 18)
        val left = setOf(9, 17, 25)

        for (cell in Slot.PITCH_RANGE) {
            val expected = when (cell) {
                in right -> Side.RIGHT
                in left -> Side.LEFT
                else -> null
            }
            assertEquals(expected, Slot(cell).requiredSide, "slot $cell")
        }

        assertEquals(
            19,
            Slot.PITCH_RANGE.count { Slot(it).requiredSide == null },
            "nineteen of the twenty five cells are central, which is why the first relaxation pass " +
                "of section 5.4 changes nothing at most cells",
        )
    }

    /**
     * The sub role column of the same table, re-read the same way and listed
     * cell by cell rather than by range, so that moving a single cell from one
     * row to the next is visible here.
     *
     * Section 3.2 names the rows and section 4.3's derivation turns each name
     * into a sub role: goleiro and zagueiros defensive, volantes 11 to 13
     * defensive, meias ofensivos 14 to 16 offensive, alas 10 and 17 the
     * offensive reading of a fullback, pontas 18 and 25 the winger reading of
     * a forward, atacantes centrais 19 to 24 the centre forward reading. The
     * laterais 2 and 9 are the one pair the table describes by side alone, and
     * they ask for no sub role; item 34 of OPEN-QUESTIONS carries why the
     * alternative reading was rejected.
     */
    @Test
    fun `every pitch cell asks for the sub role its row of section 3 point 2 names`() {
        val expected: Map<Int, PlayerStyle?> = mapOf(
            1 to PlayerStyle.DEFENSIVE,
            2 to null,
            3 to PlayerStyle.DEFENSIVE,
            4 to PlayerStyle.DEFENSIVE,
            5 to PlayerStyle.DEFENSIVE,
            6 to PlayerStyle.DEFENSIVE,
            7 to PlayerStyle.DEFENSIVE,
            8 to PlayerStyle.DEFENSIVE,
            9 to null,
            10 to PlayerStyle.OFFENSIVE,
            11 to PlayerStyle.DEFENSIVE,
            12 to PlayerStyle.DEFENSIVE,
            13 to PlayerStyle.DEFENSIVE,
            14 to PlayerStyle.OFFENSIVE,
            15 to PlayerStyle.OFFENSIVE,
            16 to PlayerStyle.OFFENSIVE,
            17 to PlayerStyle.OFFENSIVE,
            18 to PlayerStyle.WINGER,
            19 to PlayerStyle.OFFENSIVE,
            20 to PlayerStyle.OFFENSIVE,
            21 to PlayerStyle.OFFENSIVE,
            22 to PlayerStyle.OFFENSIVE,
            23 to PlayerStyle.OFFENSIVE,
            24 to PlayerStyle.OFFENSIVE,
            25 to PlayerStyle.WINGER,
        )

        assertEquals(
            Slot.PITCH_RANGE.toList(),
            expected.keys.toList(),
            "the expectation must cover the whole grid, in order",
        )
        for ((cell, style) in expected) {
            assertEquals(style, Slot(cell).requiredStyle, "slot $cell")
        }
        assertEquals(
            2,
            Slot.PITCH_RANGE.count { Slot(it).requiredStyle == PlayerStyle.WINGER },
            "winger is a sub role only a forward can hold, and only two cells ask for it, which is " +
                "the whole trigger of item 35",
        )
    }

    @Test
    fun `off pitch cells ask for no side and no sub role`() {
        for (cell in listOf(-1, 0, 26, 30, 36)) {
            assertNull(Slot(cell).requiredSide, "slot $cell")
            assertNull(Slot(cell).requiredStyle, "slot $cell")
        }
    }

    @Test
    fun `line groups follow the slot ranges`() {
        assertEquals(SlotGroup.KEEPER, Slot(1).group)
        (2..9).forEach { assertEquals(SlotGroup.DEFENCE, Slot(it).group, "slot $it") }
        (10..17).forEach { assertEquals(SlotGroup.MIDFIELD, Slot(it).group, "slot $it") }
        (19..25).forEach { assertEquals(SlotGroup.ATTACK, Slot(it).group, "slot $it") }
    }

    @Test
    fun `slot eighteen feeds no line aggregate`() {
        assertEquals(SlotGroup.NONE, Slot(18).group)
        assertEquals(Position.FORWARD, Slot(18).requiredPosition)
    }

    @Test
    fun `slot zero and bench cells feed no line aggregate`() {
        assertEquals(SlotGroup.NONE, Slot(0).group)
        assertEquals(SlotGroup.NONE, Slot(-1).group)
        assertEquals(SlotGroup.NONE, Slot(30).group)
    }

    @Test
    fun `pitch and bench ranges are distinct`() {
        assertTrue(Slot(1).isPitch)
        assertTrue(Slot(25).isPitch)
        assertTrue(Slot(26).isBench)
        assertTrue(Slot(36).isBench)
        assertTrue(!Slot(26).isPitch)
        assertTrue(!Slot(0).isPitch)
    }

    @Test
    fun `neutral ground applies only to the club world cup and national teams`() {
        assertTrue(CompetitionKind.CLUB_WORLD_CUP.isNeutralGround)
        assertTrue(CompetitionKind.NATIONAL_TEAM.isNeutralGround)
        assertTrue(!CompetitionKind.NATIONAL_LEAGUE.isNeutralGround)
        assertTrue(!CompetitionKind.CONTINENTAL_PRIMARY.isNeutralGround)
    }

    @Test
    fun `ordinals match the original data file encoding`() {
        assertEquals(Position.GOALKEEPER, Position.ofOrdinal(0))
        assertEquals(Position.FORWARD, Position.ofOrdinal(4))
        assertEquals(Side.RIGHT, Side.ofOrdinal(0))
        assertEquals(Side.LEFT, Side.ofOrdinal(1))
        assertEquals(CompetitionKind.NATIONAL_LEAGUE, CompetitionKind.ofOrdinal(1))
        assertEquals(CompetitionKind.STATE, CompetitionKind.ofOrdinal(3))
        assertEquals(CompetitionKind.FRIENDLY_TOURNAMENT, CompetitionKind.ofOrdinal(15))
    }
}
