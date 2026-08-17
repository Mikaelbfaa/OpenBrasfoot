package org.openfoot.engine.match

import org.openfoot.model.Attr
import org.openfoot.model.SpecRef

/**
 * How much each of the seven abilities counts towards a player's rating, chosen
 * by the pitch cell he occupies rather than by his natural position.
 *
 * Every row sums to one. A cell with no row, which includes slot zero, the
 * unused substitute marker and every bench cell, contributes nothing.
 */
@SpecRef("3.3")
object SlotWeights {

    private val NONE = DoubleArray(Attr.COUNT)

    private val KEEPER = weights(
        goalkeeping = 0.60,
        pace = 0.15,
        technique = 0.15,
        passing = 0.10,
    )

    private val CENTREBACK = weights(
        pace = 0.25,
        technique = 0.10,
        passing = 0.10,
        tackling = 0.50,
        playmaking = 0.05,
    )

    private val FULLBACK = weights(
        pace = 0.10,
        technique = 0.10,
        passing = 0.30,
        tackling = 0.40,
        playmaking = 0.05,
        finishing = 0.05,
    )

    private val HOLDING_MIDFIELDER = weights(
        pace = 0.15,
        technique = 0.10,
        passing = 0.20,
        tackling = 0.40,
        playmaking = 0.10,
        finishing = 0.05,
    )

    private val ATTACKING_MIDFIELDER = weights(
        pace = 0.10,
        technique = 0.10,
        passing = 0.25,
        tackling = 0.05,
        playmaking = 0.40,
        finishing = 0.10,
    )

    private val WING_BACK = weights(
        pace = 0.25,
        technique = 0.15,
        passing = 0.25,
        tackling = 0.05,
        playmaking = 0.20,
        finishing = 0.10,
    )

    private val CENTRE_FORWARD = weights(
        pace = 0.25,
        technique = 0.25,
        passing = 0.05,
        playmaking = 0.05,
        finishing = 0.40,
    )

    private val WIDE_FORWARD = weights(
        pace = 0.25,
        technique = 0.15,
        passing = 0.15,
        playmaking = 0.05,
        finishing = 0.40,
    )

    /**
     * Weight row for a pitch cell, or an all zero row when the cell has none.
     */
    fun forSlot(slot: Int): DoubleArray = when (slot) {
        1 -> KEEPER
        2, 9 -> FULLBACK
        in 3..8 -> CENTREBACK
        10, 17 -> WING_BACK
        in 11..13 -> HOLDING_MIDFIELDER
        in 14..16 -> ATTACKING_MIDFIELDER
        18, 25 -> WIDE_FORWARD
        in 19..24 -> CENTRE_FORWARD
        else -> NONE
    }

    private fun weights(
        goalkeeping: Double = 0.0,
        pace: Double = 0.0,
        technique: Double = 0.0,
        passing: Double = 0.0,
        tackling: Double = 0.0,
        playmaking: Double = 0.0,
        finishing: Double = 0.0,
    ): DoubleArray {
        val row = DoubleArray(Attr.COUNT)
        row[Attr.GOALKEEPING] = goalkeeping
        row[Attr.PACE] = pace
        row[Attr.TECHNIQUE] = technique
        row[Attr.PASSING] = passing
        row[Attr.TACKLING] = tackling
        row[Attr.PLAYMAKING] = playmaking
        row[Attr.FINISHING] = finishing
        return row
    }
}
