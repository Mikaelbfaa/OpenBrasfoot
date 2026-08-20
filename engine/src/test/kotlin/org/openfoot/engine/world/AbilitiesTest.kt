package org.openfoot.engine.world

import org.openfoot.model.Attr
import org.openfoot.model.PlayerStyle
import org.openfoot.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every row of the section 4.2 table, worked out by hand with the draws
 * scripted to zero so the formula itself is what is being read.
 *
 * The standing example is a club quality seed of fifteen, a band of seven and a
 * player of strength fifty, which makes C five.
 */
class AbilitiesTest {

    private val seed = 15
    private val band = 7
    private val strength = 50

    private fun abilities(
        position: Position,
        style: PlayerStyle = PlayerStyle.DEFENSIVE,
        strength: Int = this.strength,
        vararg draws: Int,
    ) = generateAbilities(position, style, strength, seed, band, ScriptedInts(*draws))

    private val noDraws = IntArray(7)

    private fun expect(
        goalkeeping: Int,
        pace: Int,
        technique: Int,
        passing: Int,
        tackling: Int,
        playmaking: Int,
        finishing: Int,
    ) = intArrayOf(goalkeeping, pace, technique, passing, tackling, playmaking, finishing)

    private fun assertRow(expected: IntArray, actual: IntArray) {
        for (index in 0 until Attr.COUNT) {
            assertEquals(expected[index], actual[index], "ability index $index")
        }
    }

    @Test
    fun `a goalkeeper takes his keeping from strength and the rest from the club`() {
        assertRow(
            expect(goalkeeping = 50, pace = 15, technique = 15, passing = 15, tackling = 7, playmaking = 7, finishing = 7),
            abilities(Position.GOALKEEPER, draws = noDraws),
        )
    }

    @Test
    fun `a goalkeeper draws each of his three club abilities separately`() {
        val actual = abilities(Position.GOALKEEPER, draws = intArrayOf(1, 2, 3, 3, 1, 2, 0))
        assertRow(
            expect(goalkeeping = 51, pace = 17, technique = 18, passing = 18, tackling = 8, playmaking = 9, finishing = 7),
            actual,
        )
    }

    @Test
    fun `a centreback takes nine tenths of his strength as tackling`() {
        assertRow(
            expect(goalkeeping = 1, pace = 22, technique = 22, passing = 22, tackling = 45, playmaking = 15, finishing = 7),
            abilities(Position.CENTREBACK, draws = noDraws),
        )
    }

    @Test
    fun `a defensive fullback takes four fifths of his strength as tackling`() {
        assertRow(
            expect(goalkeeping = 1, pace = 22, technique = 15, passing = 15, tackling = 40, playmaking = 7, finishing = 7),
            abilities(Position.FULLBACK, PlayerStyle.DEFENSIVE, draws = noDraws),
        )
    }

    @Test
    fun `an offensive fullback takes half his strength as playmaking`() {
        assertRow(
            expect(goalkeeping = 1, pace = 22, technique = 20, passing = 20, tackling = 15, playmaking = 25, finishing = 22),
            abilities(Position.FULLBACK, PlayerStyle.OFFENSIVE, draws = noDraws),
        )
    }

    @Test
    fun `a defensive midfielder takes seven tenths of his strength as tackling`() {
        assertRow(
            expect(goalkeeping = 1, pace = 22, technique = 15, passing = 15, tackling = 35, playmaking = 15, finishing = 15),
            abilities(Position.MIDFIELDER, PlayerStyle.DEFENSIVE, draws = noDraws),
        )
    }

    @Test
    fun `a playmaker takes his whole strength as playmaking`() {
        assertRow(
            expect(goalkeeping = 1, pace = 20, technique = 20, passing = 22, tackling = 15, playmaking = 50, finishing = 20),
            abilities(Position.MIDFIELDER, PlayerStyle.OFFENSIVE, draws = noDraws),
        )
    }

    @Test
    fun `a forward takes four fifths of his strength as finishing`() {
        assertRow(
            expect(goalkeeping = 1, pace = 20, technique = 20, passing = 22, tackling = 7, playmaking = 22, finishing = 40),
            abilities(Position.FORWARD, draws = noDraws),
        )
    }

    @Test
    fun `the primary rounding is half up at the boundary`() {
        val centreback = abilities(Position.CENTREBACK, strength = 45, draws = noDraws)
        assertEquals(41, centreback[Attr.TACKLING])

        val fullback = abilities(Position.FULLBACK, PlayerStyle.OFFENSIVE, strength = 45, draws = noDraws)
        assertEquals(23, fullback[Attr.PLAYMAKING])
    }

    @Test
    fun `seven tenths of forty five falls just short of the halfway point`() {
        val midfielder = abilities(Position.MIDFIELDER, PlayerStyle.DEFENSIVE, strength = 45, draws = noDraws)
        assertEquals(
            31,
            midfielder[Attr.TACKLING],
            "decimal arithmetic would give 31.5 and round to 32, but seven tenths has no exact " +
                "binary form and the product lands at 31.499999999999996, so it rounds down",
        )
        assertTrue(
            45 * 0.7 < 31.5,
            "if this ever stops holding, every defensive midfielder in every save shifts by one",
        )
    }

    @Test
    fun `every row assigns all seven abilities and draws exactly seven times`() {
        val rows = listOf(
            Position.GOALKEEPER to PlayerStyle.DEFENSIVE,
            Position.CENTREBACK to PlayerStyle.DEFENSIVE,
            Position.FULLBACK to PlayerStyle.DEFENSIVE,
            Position.FULLBACK to PlayerStyle.OFFENSIVE,
            Position.MIDFIELDER to PlayerStyle.DEFENSIVE,
            Position.MIDFIELDER to PlayerStyle.OFFENSIVE,
            Position.FORWARD to PlayerStyle.WINGER,
        )
        for ((position, style) in rows) {
            val rng = ScriptedInts(*IntArray(7))
            val row = generateAbilities(position, style, strength, seed, band, rng)
            assertEquals(7, rng.draws, "$position $style draw count")
            assertTrue(row.all { it > 0 }, "$position $style left an ability at zero")
        }
    }

    @Test
    fun `the three rows the spec leaves without keeping get the fullback spread`() {
        val rows = listOf(
            Triple(Position.FULLBACK, PlayerStyle.OFFENSIVE, 6),
            Triple(Position.MIDFIELDER, PlayerStyle.DEFENSIVE, 6),
            Triple(Position.MIDFIELDER, PlayerStyle.OFFENSIVE, 6),
        )
        for ((position, style, keepingDrawIndex) in rows) {
            val draws = IntArray(7)
            draws[keepingDrawIndex] = 3
            val row = generateAbilities(position, style, strength, seed, band, ScriptedInts(*draws))
            assertEquals(4, row[Attr.GOALKEEPING], "$position $style keeping")
        }
    }

    @Test
    fun `only the primary ability moves when strength moves`() {
        val rows = listOf(
            Triple(Position.GOALKEEPER, PlayerStyle.DEFENSIVE, Attr.GOALKEEPING),
            Triple(Position.CENTREBACK, PlayerStyle.DEFENSIVE, Attr.TACKLING),
            Triple(Position.FULLBACK, PlayerStyle.DEFENSIVE, Attr.TACKLING),
            Triple(Position.FULLBACK, PlayerStyle.OFFENSIVE, Attr.PLAYMAKING),
            Triple(Position.MIDFIELDER, PlayerStyle.DEFENSIVE, Attr.TACKLING),
            Triple(Position.MIDFIELDER, PlayerStyle.OFFENSIVE, Attr.PLAYMAKING),
            Triple(Position.FORWARD, PlayerStyle.OFFENSIVE, Attr.FINISHING),
        )
        for ((position, style, primary) in rows) {
            val weak = generateAbilities(position, style, 20, seed, band, ScriptedInts(*IntArray(7)))
            val strong = generateAbilities(position, style, 80, seed, band, ScriptedInts(*IntArray(7)))
            for (index in 0 until Attr.COUNT) {
                if (index == primary) {
                    assertTrue(strong[index] > weak[index], "$position $style primary did not move")
                } else {
                    assertEquals(weak[index], strong[index], "$position $style ability $index moved")
                }
            }
        }
    }
}
