package org.openfoot.importer

import org.openfoot.dataset.ClubEntry
import org.openfoot.model.Country
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class LeagueConfigTest {

    private class Tier(
        val pais: Int,
        val divisao: Int,
        val nTimes: Int,
    ) : Serializable

    private class Pyramid(val a: ArrayList<Tier>) : Serializable

    private fun bytes(value: Any): ByteArray {
        val out = ByteArrayOutputStream()
        ObjectOutputStream(out).use { it.writeObject(value) }
        return out.toByteArray()
    }

    private fun club(ref: String, level: Int, country: Int = Country.BRAZIL) = ClubEntry(
        ref = ref,
        name = ref,
        country = country,
        level = level,
        reputation = 2,
    )

    @Test
    fun `a configuration yields one shape per tier`() {
        val shapes = LeagueConfigReader.read(
            bytes(Pyramid(arrayListOf(Tier(29, 1, 20), Tier(29, 2, 20), Tier(29, 3, 20)))),
        )
        assertEquals(3, shapes.size)
        assertEquals(DivisionShape(29, 1, 20), shapes.first())
    }

    @Test
    fun `a tier with no teams is not a tier`() {
        assertEquals(
            listOf(DivisionShape(29, 1, 20)),
            LeagueConfigReader.read(bytes(Pyramid(arrayListOf(Tier(29, 1, 20), Tier(29, 2, 0))))),
        )
    }

    @Test
    fun `something that is not a configuration is refused`() {
        assertFailsWith<IllegalArgumentException> { LeagueConfigReader.read(bytes(Tier(29, 1, 20))) }
    }

    @Test
    fun `the strongest clubs fill the first division`() {
        val clubs = listOf(
            club("fraco", 8),
            club("forte", 19),
            club("medio", 14),
            club("outro", 16),
        )
        val assigned = assignDivisions(clubs, listOf(DivisionShape(Country.BRAZIL, 1, 2), DivisionShape(Country.BRAZIL, 2, 2)))
        val byRef = assigned.associateBy { it.ref }

        assertEquals(1, byRef["forte"]?.division)
        assertEquals(1, byRef["outro"]?.division)
        assertEquals(2, byRef["medio"]?.division)
        assertEquals(2, byRef["fraco"]?.division)
    }

    @Test
    fun `a club beyond the last tier keeps no division`() {
        val clubs = listOf(club("um", 19), club("dois", 18), club("tres", 17))
        val assigned = assignDivisions(clubs, listOf(DivisionShape(Country.BRAZIL, 1, 2)))
        assertNull(assigned.single { it.ref == "tres" }.division)
    }

    @Test
    fun `clubs of another country are left alone`() {
        val clubs = listOf(club("bra", 19), club("esp", 20, country = 65))
        val assigned = assignDivisions(clubs, listOf(DivisionShape(Country.BRAZIL, 1, 5)))
        assertEquals(1, assigned.single { it.ref == "bra" }.division)
        assertNull(assigned.single { it.ref == "esp" }.division)
    }

    @Test
    fun `equal levels are ordered by reference so a pyramid is reproducible`() {
        val clubs = listOf(club("zeta", 10), club("alfa", 10), club("beta", 10))
        val shapes = listOf(DivisionShape(Country.BRAZIL, 1, 1), DivisionShape(Country.BRAZIL, 2, 2))

        val once = assignDivisions(clubs, shapes).associate { it.ref to it.division }
        val again = assignDivisions(clubs.reversed(), shapes).associate { it.ref to it.division }

        assertEquals(1, once["alfa"])
        assertEquals(once, again)
    }

    @Test
    fun `a national team is never given a division`() {
        val selection = ClubEntry(
            ref = "selecao",
            name = "Selecao",
            country = Country.BRAZIL,
            level = 20,
            reputation = 5,
            nationalTeam = true,
        )
        val assigned = assignDivisions(listOf(selection, club("clube", 8)), listOf(DivisionShape(Country.BRAZIL, 1, 5)))
        assertNull(assigned.single { it.ref == "selecao" }.division)
        assertEquals(1, assigned.single { it.ref == "clube" }.division)
    }
}
