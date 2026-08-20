package org.openfoot.engine.lineup

import org.openfoot.model.Rng
import org.openfoot.model.Slot
import org.openfoot.model.SpecRef
import org.openfoot.model.rand

/**
 * One of the formations the original ships: a menu name and the list of pitch
 * cells that seats its eleven players.
 */
@SpecRef("5.1")
data class Formation(val id: Int, val name: String, val slots: List<Slot>)

/**
 * The twelve formations of the original, transcribed from the section 5.1
 * table in the order that table gives them.
 *
 * The order within each slot list is load bearing, not incidental. Every list
 * names the keeper first, then the forwards, then the midfielders, and the
 * defenders last. Section 5.4's automatic lineup fills a formation's slots by
 * walking a pool of candidates ordered strongest first, taking the first
 * compatible player for each slot in list order. Because these lists put the
 * forwards ahead of the defenders, the strongest available players land at
 * forward and the defence gets whoever is left, which is exactly the AI
 * behaviour the original shows. Sorting a list by slot number would silently
 * change which players the AI fields.
 */
@SpecRef("5.1")
object Formations {

    val ALL: List<Formation> = listOf(
        Formation(1, "5-4-1", slotsOf(1, 20, 11, 13, 14, 16, 2, 9, 6, 4, 8)),
        Formation(2, "5-3-2", slotsOf(1, 22, 24, 12, 14, 16, 2, 9, 6, 4, 8)),
        Formation(3, "4-5-1", slotsOf(1, 23, 11, 13, 15, 2, 9, 6, 8, 10, 17)),
        Formation(4, "4-4-2", slotsOf(1, 22, 24, 11, 13, 14, 16, 2, 9, 3, 5)),
        Formation(5, "4-4-2 def", slotsOf(1, 19, 21, 11, 12, 13, 15, 2, 9, 6, 8)),
        Formation(6, "4-4-2 ofensivo", slotsOf(1, 22, 24, 12, 14, 15, 16, 2, 9, 6, 8)),
        Formation(7, "4-3-3", slotsOf(1, 22, 23, 24, 12, 14, 16, 2, 9, 6, 8)),
        Formation(8, "4-3-3 def", slotsOf(1, 19, 20, 21, 11, 13, 15, 2, 9, 6, 8)),
        Formation(9, "3-5-2", slotsOf(1, 22, 24, 11, 13, 15, 4, 6, 8, 10, 17)),
        Formation(10, "3-4-3", slotsOf(1, 18, 25, 23, 11, 13, 4, 6, 8, 10, 17)),
        Formation(11, "4-2-3-1", slotsOf(1, 23, 14, 16, 15, 13, 11, 2, 9, 6, 8)),
        Formation(12, "4-2-3-1 Alas", slotsOf(1, 20, 10, 17, 15, 13, 11, 2, 9, 6, 8)),
    )

    /**
     * Looks a formation up by its menu id, one to twelve.
     */
    fun byId(id: Int): Formation = ALL.first { it.id == id }

    private fun slotsOf(vararg values: Int): List<Slot> = values.map(::Slot)
}

/**
 * Draws the formation the AI fields for a match, following section 3.2's
 * rand(1..100) table.
 *
 * The table is written one based, drawing a number from one to a hundred
 * inclusive and mapping bands of that number to a formation. The engine's
 * rand(bound) is nought based instead, returning zero until bound exclusive,
 * so this function converts once, at the very top, by drawing rand(100) and
 * adding one, rather than adjusting every band boundary below by hand.
 *
 * The table only ever names eleven formations: formation twelve, the widened
 * variant with wing backs, is a menu only choice the automatic lineup never
 * draws, which is why its band is missing rather than merely narrow.
 */
@SpecRef("3.2")
fun drawFormation(rng: Rng): Formation {
    val draw = rng.rand(100) + 1
    val id = when (draw) {
        in 1..2 -> 1
        in 3..4 -> 2
        in 5..7 -> 3
        in 8..38 -> 4
        in 39..49 -> 5
        in 50..60 -> 6
        in 61..65 -> 7
        in 66..72 -> 8
        in 73..90 -> 9
        in 91..92 -> 10
        else -> 11
    }
    return Formations.byId(id)
}
