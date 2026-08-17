package org.openfoot.engine.world

import org.openfoot.dataset.ClubEntry
import org.openfoot.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Section 4.9, anchored on the calibration the spec supplies itself: strength
 * fifty, age twenty four, club level twenty, giving ten thousand times seven
 * hundred and seventy six, or 7.76 million.
 */
class MarketValueTest {

    private fun value(
        strength: Int = 50,
        age: Int = 24,
        position: Position = Position.MIDFIELDER,
        starter: Boolean = false,
        star: Boolean = false,
        topWorld: Boolean = false,
        clubLevel: Int = 20,
        europeanNationality: Boolean = false,
    ) = marketValue(strength, age, position, starter, star, topWorld, clubLevel, europeanNationality)

    @Test
    fun `the calibration the spec gives reproduces exactly`() {
        assertEquals(7_760_000L, value())
    }

    @Test
    fun `value grows with the square of strength`() {
        val single = value(strength = 25)
        val double = value(strength = 50)
        assertEquals(4 * single, double)
    }

    @Test
    fun `the club level base steps at the documented levels`() {
        assertEquals(7_760_000L, value(clubLevel = 20))
        assertEquals(6_760_000L, value(clubLevel = 18))
        assertEquals(5_760_000L, value(clubLevel = 12))
        assertEquals(5_420_000L, value(clubLevel = 6))
    }

    @Test
    fun `the age term peaks young and turns negative past thirty four`() {
        assertEquals(351, ageTerm(19))
        assertEquals(176, ageTerm(24))
        assertEquals(30, ageTerm(30))
        assertEquals(10, ageTerm(33))
        assertEquals(0, ageTerm(34))
        assertEquals(-300, ageTerm(40))
    }

    @Test
    fun `an age below sixteen is valued as sixteen`() {
        assertEquals(ageTerm(16), ageTerm(15))
        assertEquals(ageTerm(16), ageTerm(1))
        assertEquals(10_320_000L, value(age = 15))
    }

    @Test
    fun `the age bands meet without a gap at each boundary`() {
        assertEquals((32 - 19) * 27, ageTerm(19))
        assertEquals((32 - 20) * 22, ageTerm(20))
        assertEquals((32 - 25) * 22, ageTerm(25))
        assertEquals((32 - 26) * 15, ageTerm(26))
        assertEquals((32 - 31) * 15, ageTerm(31))
        assertEquals((34 - 32) * 10, ageTerm(32))
    }

    @Test
    fun `a base driven to nothing by age collapses to the floor`() {
        assertEquals(600_000L, value(age = 48, clubLevel = 6))
        assertTrue(clubLevelBaseWouldBeNegative(age = 48, clubLevel = 6))
    }

    private fun clubLevelBaseWouldBeNegative(age: Int, clubLevel: Int): Boolean {
        val base = if (clubLevel >= 12) 400 else 366
        return base + ageTerm(age) <= 0
    }

    @Test
    fun `the modifiers multiply the finished value`() {
        assertEquals(9_312_000L, value(starter = true))
        assertEquals(10_088_000L, value(position = Position.FORWARD))
        assertEquals(13_192_000L, value(star = true))
    }

    @Test
    fun `top world stacks on top of the star multiplier`() {
        assertEquals(21_107_200L, value(star = true, topWorld = true))
    }

    @Test
    fun `no club level the dataset admits reaches the higher star rungs`() {
        for (level in ClubEntry.LEVEL_RANGE) {
            assertEquals(
                1.7,
                starMultiplier(level, europeanNationality = true),
                "club level $level reached a star rung that needs level twenty one or more",
            )
        }
    }

    @Test
    fun `a European nationality alone changes nothing at reachable club levels`() {
        assertEquals(
            value(star = true, europeanNationality = false),
            value(star = true, europeanNationality = true),
        )
    }
}
