package org.openfoot.engine.world

import org.openfoot.dataset.ClubEntry
import org.openfoot.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Section 4.8 worked by hand. The standing example is a midfielder of strength
 * fifty, aged twenty five, at a first division club of level eighteen outside
 * the five countries that pay more, on weekly wages: base six hundred, halved
 * to three hundred, and a core of thirty thousand.
 */
class SalaryTest {

    private fun wage(
        strength: Int = 50,
        age: Int = 25,
        position: Position = Position.MIDFIELDER,
        star: Boolean = false,
        topWorld: Boolean = false,
        clubLevel: Int = 18,
        division: Int? = 1,
        majorLeagueCountry: Boolean = false,
        monthlyWages: Boolean = false,
    ) = salary(
        strength, age, position, star, topWorld,
        clubLevel, division, majorLeagueCountry, monthlyWages,
    )

    @Test
    fun `the base case is strength times two times the halved division base`() {
        assertEquals(30_000L, wage())
    }

    @Test
    fun `the monthly option simply quadruples the weekly figure`() {
        assertEquals(120_000L, wage(monthlyWages = true))
        assertEquals(wage() * 4, wage(monthlyWages = true))
    }

    @Test
    fun `the five paying countries lift the first division base`() {
        assertEquals(37_500L, wage(majorLeagueCountry = true))
    }

    @Test
    fun `a goalkeeper is cheaper than a midfielder of identical strength`() {
        assertEquals(26_500L, wage(position = Position.GOALKEEPER))
        assertTrue(wage(position = Position.GOALKEEPER) < wage())
    }

    @Test
    fun `the position adjustments follow the spec order of cheapness`() {
        assertEquals(-70, positionAdjustment(Position.GOALKEEPER))
        assertEquals(-50, positionAdjustment(Position.FORWARD))
        assertEquals(-40, positionAdjustment(Position.CENTREBACK))
        assertEquals(-30, positionAdjustment(Position.FULLBACK))
        assertEquals(0, positionAdjustment(Position.MIDFIELDER))
    }

    @Test
    fun `age subtracts only past thirty two`() {
        assertEquals(wage(age = 25), wage(age = 31))
        assertEquals(30_000L, wage(age = DECLINE_AGE))
        assertEquals(29_100L, wage(age = 35))
    }

    @Test
    fun `a star earns his strength times two hundred and fifty on top`() {
        assertEquals(42_500L, wage(star = true))
    }

    @Test
    fun `top world takes the star bonus and then multiplies the whole wage`() {
        assertEquals(59_500L, wage(star = true, topWorld = true))
    }

    @Test
    fun `the floor catches a player the age penalty would drive negative`() {
        assertEquals(500L, wage(strength = 1, age = 45, clubLevel = 6, division = 9))
    }

    @Test
    fun `a national team squad falls to the lowest base`() {
        assertEquals(wage(division = null), wage(division = 9))
    }

    @Test
    fun `a forward in a third division of a paying country`() {
        assertEquals(22_500L, wage(position = Position.FORWARD, division = 3, majorLeagueCountry = true))
    }

    @Test
    fun `no club level the dataset admits earns the rich club premium`() {
        for (level in ClubEntry.LEVEL_RANGE) {
            assertTrue(
                level <= RICH_CLUB_LEVEL,
                "club level $level would take the premium, which no data file expresses",
            )
        }
        assertEquals(wage(clubLevel = RICH_CLUB_LEVEL), wage(clubLevel = ClubEntry.LEVEL_RANGE.last))
    }
}
