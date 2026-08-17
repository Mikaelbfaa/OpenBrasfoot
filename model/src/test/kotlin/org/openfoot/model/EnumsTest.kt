package org.openfoot.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class EnumsTest {

    @Test
    fun `trait ordinals match the original data file encoding`() {
        assertEquals(Trait.POSITIONING, Trait.ofOrdinal(0))
        assertEquals(Trait.PENALTY_SAVING, Trait.ofOrdinal(1))
        assertEquals(Trait.REFLEXES, Trait.ofOrdinal(2))
        assertEquals(Trait.RUSHING_OUT, Trait.ofOrdinal(3))
        assertEquals(Trait.PLAYMAKING, Trait.ofOrdinal(4))
        assertEquals(Trait.HEADING, Trait.ofOrdinal(5))
        assertEquals(Trait.CROSSING, Trait.ofOrdinal(6))
        assertEquals(Trait.TACKLING, Trait.ofOrdinal(7))
        assertEquals(Trait.DRIBBLING, Trait.ofOrdinal(8))
        assertEquals(Trait.FINISHING, Trait.ofOrdinal(9))
        assertEquals(Trait.MARKING, Trait.ofOrdinal(10))
        assertEquals(Trait.PASSING, Trait.ofOrdinal(11))
        assertEquals(Trait.STAMINA, Trait.ofOrdinal(12))
        assertEquals(Trait.PACE, Trait.ofOrdinal(13))
    }

    @Test
    fun `there are exactly fourteen traits`() {
        assertEquals(14, Trait.entries.size)
    }

    @Test
    fun `the first four traits are the goalkeeping ones`() {
        assertTrue(Trait.POSITIONING.isGoalkeeping)
        assertTrue(Trait.RUSHING_OUT.isGoalkeeping)
        assertFalse(Trait.PLAYMAKING.isGoalkeeping)
        assertFalse(Trait.PACE.isGoalkeeping)
    }

    @Test
    fun `an unknown trait ordinal is rejected`() {
        try {
            Trait.ofOrdinal(14)
            fail("expected an exception for ordinal 14")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("14"))
        }
    }

    @Test
    fun `marking ordinals run from light to very heavy`() {
        assertEquals(Marking.LIGHT, Marking.ofOrdinal(0))
        assertEquals(Marking.HEAVY, Marking.ofOrdinal(1))
        assertEquals(Marking.VERY_HEAVY, Marking.ofOrdinal(2))
    }

    @Test
    fun `team sides oppose each other`() {
        assertEquals(TeamSide.AWAY, TeamSide.HOME.opponent)
        assertEquals(TeamSide.HOME, TeamSide.AWAY.opponent)
    }

    @Test
    fun `home advantage follows the possessor away from neutral ground`() {
        assertEquals(
            HomeAdvantage.POSSESSOR_HOME,
            HomeAdvantage.of(TeamSide.HOME, neutralGround = false),
        )
        assertEquals(
            HomeAdvantage.POSSESSOR_AWAY,
            HomeAdvantage.of(TeamSide.AWAY, neutralGround = false),
        )
    }

    @Test
    fun `neutral ground removes home advantage for both sides`() {
        assertEquals(HomeAdvantage.NONE, HomeAdvantage.of(TeamSide.HOME, neutralGround = true))
        assertEquals(HomeAdvantage.NONE, HomeAdvantage.of(TeamSide.AWAY, neutralGround = true))
    }
}
