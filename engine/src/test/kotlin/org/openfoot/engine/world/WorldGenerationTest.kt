package org.openfoot.engine.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The determinism contract the whole project rests on, stated as tests.
 *
 * A career replays from its seed, a bug report is a seed plus a command log,
 * and neither is true unless generation depends on the seed and on nothing
 * else at all.
 */
class WorldGenerationTest {

    private fun dataset(refs: List<String>) = WorldFixtures.dataset(
        refs.map { ref ->
            WorldFixtures.club(
                ref = ref,
                squad = List(4) { index -> WorldFixtures.player(name = "$ref jogador $index") },
            )
        },
    )

    @Test
    fun `the same seed builds the same world`() {
        val data = dataset(listOf("um", "dois", "tres"))
        assertEquals(generateWorld(data, 2026), generateWorld(data, 2026))
    }

    @Test
    fun `one bit of seed changes the world`() {
        val data = dataset(listOf("um", "dois", "tres"))
        assertNotEquals(generateWorld(data, 2026), generateWorld(data, 2027))
    }

    @Test
    fun `a club generates the same squad wherever it sits in the dataset`() {
        val forward = dataset(listOf("um", "dois", "tres"))
        val reversed = dataset(listOf("tres", "dois", "um"))

        val fromForward = generateWorld(forward, 99).club("dois")
        val fromReversed = generateWorld(reversed, 99).club("dois")

        assertEquals(fromForward, fromReversed)
    }

    @Test
    fun `adding a club leaves every other club untouched`() {
        val before = generateWorld(dataset(listOf("um", "dois")), 5)
        val after = generateWorld(dataset(listOf("um", "novo", "dois")), 5)

        assertEquals(before.club("um"), after.club("um"))
        assertEquals(before.club("dois"), after.club("dois"))
        assertEquals(3, after.clubs.size)
    }

    @Test
    fun `two clubs with different references generate differently`() {
        val world = generateWorld(dataset(listOf("um", "dois")), 5)
        assertNotEquals(
            world.club("um")?.squad?.map { it.strength },
            world.club("dois")?.squad?.map { it.strength },
        )
    }

    @Test
    fun `the world carries the seed it was built from`() {
        assertEquals(2026L, generateWorld(dataset(listOf("um")), 2026).seed)
    }

    @Test
    fun `the world counts its players and finds its clubs`() {
        val world = generateWorld(dataset(listOf("um", "dois")), 1)
        assertEquals(8, world.playerCount)
        assertEquals("um", world.club("um")?.entry?.ref)
        assertNull(world.club("ausente"))
    }

    @Test
    fun `every player in the world comes out playable`() {
        val world = generateWorld(dataset(listOf("um", "dois", "tres")), 4321)
        for (club in world.clubs) {
            for (player in club.squad) {
                assertTrue(player.strength in 1..100, "${player.name} strength ${player.strength}")
                assertTrue(
                    player.abilities.all { it in 0..100 },
                    "${player.name} abilities ${player.abilities}",
                )
            }
        }
    }

    @Test
    fun `the club key is stable for the references a dataset uses`() {
        assertEquals("clube_bra".hashCode().toLong(), clubKey("clube_bra"))
        assertNotEquals(clubKey("um"), clubKey("dois"))
    }
}
