package org.openfoot.importer

import org.openfoot.model.Country
import org.openfoot.model.Position
import org.openfoot.model.Side
import org.openfoot.model.Trait
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The decoder, driven by files this test builds itself.
 *
 * The fixtures below carry the same field names a real team file does, because
 * those names are what the decoder reads. They are this test's own classes; no
 * file from the original game is involved, and none can be, since the data is
 * not distributable and no continuous integration machine has it.
 */
class TeamFileReaderTest {

    private fun squadman(
        name: String = "Jogador",
        age: Int = 25,
        country: Int = Country.BRAZIL,
        position: Int = 3,
        firstTrait: Int = 11,
        secondTrait: Int = 12,
        status: Int = 1,
    ) = ImportFixtures.squadman(
        name = name,
        age = age,
        country = country,
        position = position,
        firstTrait = firstTrait,
        secondTrait = secondTrait,
        status = status,
    )

    private fun team(
        ref: String = "clube_bra",
        country: Int = Country.BRAZIL,
        state: Int = 18,
        version: Int = 185,
        squad: List<ImportFixtures.Squadman> = listOf(squadman()),
    ) = ImportFixtures.team(
        ref = ref,
        country = country,
        state = state,
        version = version,
        squad = squad,
    )

    private fun read(value: Any, fileRef: String = "clube_bra", notes: ImportNotes = ImportNotes()) =
        TeamFileReader.read(ImportFixtures.bytes(value), fileRef, notes)

    @Test
    fun `a well formed file becomes a club entry`() {
        val club = read(team())
        assertEquals("clube_bra", club.ref)
        assertEquals("Clube", club.name)
        assertEquals(Country.BRAZIL, club.country)
        assertEquals(18, club.level)
        assertEquals(4, club.reputation)
        assertEquals("Estadio", club.stadium)
        assertEquals(45000, club.capacity)
        assertEquals("Tecnico", club.coach)
        assertEquals(1, club.squad.size)
    }

    @Test
    fun `a player carries through with his position side and characteristics`() {
        val player = read(team()).squad.single()
        assertEquals("Jogador", player.name)
        assertEquals(25, player.age)
        assertEquals(Position.MIDFIELDER, player.position)
        assertEquals(Side.RIGHT, player.side)
        assertEquals(Trait.PASSING, player.firstTrait)
        assertEquals(Trait.STAMINA, player.secondTrait)
        assertTrue(player.starter)
        assertEquals(6, player.talent)
    }

    @Test
    fun `a status other than one is not a starter`() {
        assertTrue(!read(team(squad = listOf(squadman(status = 0)))).squad.single().starter)
    }

    @Test
    fun `a file the game itself would refuse is refused here too`() {
        val failure = assertFailsWith<IllegalArgumentException> { read(team(version = 184)) }
        assertTrue(failure.message.orEmpty().contains("185"), failure.message.orEmpty())
    }

    @Test
    fun `something that is not a team file is refused`() {
        assertFailsWith<IllegalArgumentException> { read(squadman()) }
    }

    @Test
    fun `a state outside Brazil is dropped as the stale bytes it is`() {
        assertNull(read(team(country = 65, state = 22)).state)
    }

    @Test
    fun `a Brazilian state is kept`() {
        assertEquals(18, read(team(country = Country.BRAZIL, state = 18)).state)
    }

    @Test
    fun `a Brazilian club with an impossible state loses it and says so`() {
        val notes = ImportNotes()
        val club = read(team(country = Country.BRAZIL, state = 40), notes = notes)
        assertNull(club.state)
        assertTrue(notes.notes.single().contains("state 40"), notes.notes.toString())
    }

    @Test
    fun `an impossible age is corrected rather than losing the player`() {
        val notes = ImportNotes()
        val club = read(team(squad = listOf(squadman(name = "Bebe", age = 0))), notes = notes)
        assertEquals(15, club.squad.single().age)
        assertTrue(notes.notes.single().contains("Bebe"), notes.notes.toString())
    }

    @Test
    fun `an unusable player is dropped without costing the club`() {
        val notes = ImportNotes()
        val club = read(
            team(
                squad = listOf(
                    squadman(name = "Bom"),
                    squadman(name = "Impossivel", position = 4, firstTrait = 2),
                ),
            ),
            notes = notes,
        )
        assertEquals(listOf("Bom"), club.squad.map { it.name })
        assertTrue(notes.notes.single().contains("Impossivel"), notes.notes.toString())
    }

    @Test
    fun `a renamed file keeps the reference the format stores and reports it`() {
        val notes = ImportNotes()
        val club = read(team(ref = "verdadeiro"), fileRef = "renomeado", notes = notes)
        assertEquals("verdadeiro", club.ref)
        assertTrue(notes.notes.single().contains("renamed"), notes.notes.toString())
    }

    @Test
    fun `the squad keeps the order the file lists it in`() {
        val club = read(
            team(squad = listOf(squadman(name = "Um"), squadman(name = "Dois"), squadman(name = "Tres"))),
        )
        assertEquals(listOf("Um", "Dois", "Tres"), club.squad.map { it.name })
    }

    @Test
    fun `a clean file produces no notes at all`() {
        val notes = ImportNotes()
        read(team(), notes = notes)
        assertEquals(emptyList(), notes.notes)
    }
}
