package org.openbrasfoot.model

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
