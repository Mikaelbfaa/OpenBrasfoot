package org.openfoot.engine.world

import org.openfoot.model.Position
import org.openfoot.model.Trait
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Section 4.3 is a chain of ordered tests rather than a table, so these pin the
 * order as much as the outcomes. The interesting cases are the ones where a
 * player carries one trait from each side of the chain.
 */
class PlayerStyleTest {

    private fun style(position: Position, first: Trait, second: Trait = Trait.STAMINA) =
        playerStyle(position, first, second)

    @Test
    fun `goalkeepers and centrebacks have no style to choose`() {
        for (trait in listOf(Trait.PACE, Trait.DRIBBLING, Trait.MARKING)) {
            assertEquals(
                PlayerStyle.DEFENSIVE,
                style(Position.CENTREBACK, trait),
                "centreback with $trait",
            )
        }
        assertEquals(PlayerStyle.DEFENSIVE, style(Position.GOALKEEPER, Trait.REFLEXES, Trait.POSITIONING))
    }

    @Test
    fun `a fullback with pace or crossing is offensive`() {
        assertEquals(PlayerStyle.OFFENSIVE, style(Position.FULLBACK, Trait.PACE))
        assertEquals(PlayerStyle.OFFENSIVE, style(Position.FULLBACK, Trait.CROSSING))
    }

    @Test
    fun `a fullback with tackling or marking is defensive`() {
        assertEquals(PlayerStyle.DEFENSIVE, style(Position.FULLBACK, Trait.TACKLING))
        assertEquals(PlayerStyle.DEFENSIVE, style(Position.FULLBACK, Trait.MARKING))
    }

    @Test
    fun `pace beats marking for a fullback because it is tested first`() {
        assertEquals(PlayerStyle.OFFENSIVE, playerStyle(Position.FULLBACK, Trait.PACE, Trait.MARKING))
        assertEquals(PlayerStyle.OFFENSIVE, playerStyle(Position.FULLBACK, Trait.MARKING, Trait.PACE))
    }

    @Test
    fun `a fullback with only creative traits is offensive by the third test`() {
        for (trait in listOf(Trait.DRIBBLING, Trait.FINISHING, Trait.PASSING, Trait.PLAYMAKING)) {
            assertEquals(PlayerStyle.OFFENSIVE, style(Position.FULLBACK, trait), "fullback with $trait")
        }
    }

    @Test
    fun `a fullback matching no test at all falls back to defensive`() {
        assertEquals(
            PlayerStyle.DEFENSIVE,
            playerStyle(Position.FULLBACK, Trait.HEADING, Trait.STAMINA),
        )
    }

    @Test
    fun `a midfielder with creative traits is offensive`() {
        for (trait in listOf(Trait.PASSING, Trait.FINISHING, Trait.DRIBBLING, Trait.PLAYMAKING)) {
            assertEquals(PlayerStyle.OFFENSIVE, style(Position.MIDFIELDER, trait), "midfielder with $trait")
        }
    }

    @Test
    fun `a midfielder with tackling or marking alone is defensive`() {
        assertEquals(PlayerStyle.DEFENSIVE, style(Position.MIDFIELDER, Trait.TACKLING))
        assertEquals(PlayerStyle.DEFENSIVE, style(Position.MIDFIELDER, Trait.MARKING))
    }

    @Test
    fun `passing beats marking for a midfielder because it is tested first`() {
        assertEquals(
            PlayerStyle.OFFENSIVE,
            playerStyle(Position.MIDFIELDER, Trait.PASSING, Trait.MARKING),
        )
    }

    @Test
    fun `a midfielder matching no test defaults to offensive`() {
        assertEquals(
            PlayerStyle.OFFENSIVE,
            playerStyle(Position.MIDFIELDER, Trait.HEADING, Trait.STAMINA),
        )
    }

    @Test
    fun `a forward with tackling or marking is defensive before anything else`() {
        assertEquals(PlayerStyle.DEFENSIVE, style(Position.FORWARD, Trait.TACKLING))
        assertEquals(
            PlayerStyle.DEFENSIVE,
            playerStyle(Position.FORWARD, Trait.MARKING, Trait.DRIBBLING),
        )
    }

    @Test
    fun `a forward with dribbling pace or crossing is a winger`() {
        assertEquals(PlayerStyle.WINGER, style(Position.FORWARD, Trait.DRIBBLING))
        assertEquals(PlayerStyle.WINGER, style(Position.FORWARD, Trait.PACE))
        assertEquals(PlayerStyle.WINGER, style(Position.FORWARD, Trait.CROSSING))
    }

    @Test
    fun `a forward matching no test defaults to offensive`() {
        assertEquals(
            PlayerStyle.OFFENSIVE,
            playerStyle(Position.FORWARD, Trait.HEADING, Trait.STAMINA),
        )
    }

    @Test
    fun `a repeated trait decides the same way as a single one`() {
        assertEquals(
            playerStyle(Position.FULLBACK, Trait.PACE, Trait.STAMINA),
            playerStyle(Position.FULLBACK, Trait.PACE, Trait.PACE),
        )
    }
}
