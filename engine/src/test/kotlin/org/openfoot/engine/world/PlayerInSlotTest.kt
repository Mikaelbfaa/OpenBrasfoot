package org.openfoot.engine.world

import org.openfoot.model.PlayerId
import org.openfoot.model.PlayerStyle
import org.openfoot.model.Position
import org.openfoot.model.Side
import org.openfoot.model.Slot
import org.openfoot.model.Trait
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The bridge from a generated player to a player in a cell carries exactly the
 * facts the match engine reads, and the two that were added for match state:
 * the identity that energy and bookings are keyed by, and the age that section
 * 3.9 drains energy by.
 */
class PlayerInSlotTest {

    private fun generated(age: Int) = Player(
        name = "Teste",
        age = age,
        country = 3,
        position = Position.MIDFIELDER,
        side = Side.RIGHT,
        firstTrait = Trait.PASSING,
        secondTrait = Trait.STAMINA,
        starter = true,
        star = false,
        topWorld = false,
        talent = 5,
        style = PlayerStyle.OFFENSIVE,
        strength = 60,
        abilities = List(7) { 60 },
        contractDays = 700,
        salary = 1000L,
        marketValue = 100_000L,
    )

    @Test
    fun `the identity handed in is the identity carried`() {
        val inSlot = generated(age = 24).inSlot(Slot(13), PlayerId(9))
        assertEquals(PlayerId(9), inSlot.id)
    }

    @Test
    fun `age crosses the bridge because section 3 9 drains by it`() {
        val inSlot = generated(age = 37).inSlot(Slot(13), PlayerId(0))
        assertEquals(37, inSlot.age)
    }
}
