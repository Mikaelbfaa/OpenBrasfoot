package org.openbrasfoot.model

/**
 * A position on the twenty five cell pitch grid.
 *
 * The slot number lives on the player rather than the other way round, matching
 * the original. Zero means unassigned, one to twenty five are pitch cells,
 * twenty six to thirty six are bench cells, and minus one is the value the
 * original leaves on an unused substitute.
 *
 * Slot eighteen is deliberately outside every line group. That is a defect in
 * the original, reproduced under the classic ruleset and corrected under the
 * modern one, so a player standing there contributes to no aggregate.
 */
@JvmInline
@SpecRef("3.2")
value class Slot(val value: Int) {

    val isPitch: Boolean get() = value in PITCH_RANGE

    val isBench: Boolean get() = value in BENCH_RANGE

    /**
     * The natural position this cell asks for, or null for cells that ask for
     * nothing because they are not on the pitch.
     */
    val requiredPosition: Position?
        get() = when (value) {
            1 -> Position.GOALKEEPER
            2, 9, 10, 17 -> Position.FULLBACK
            in 3..8 -> Position.CENTREBACK
            in 11..16 -> Position.MIDFIELDER
            in 18..25 -> Position.FORWARD
            else -> null
        }

    val group: SlotGroup
        get() = when (value) {
            1 -> SlotGroup.KEEPER
            in 2..9 -> SlotGroup.DEFENCE
            in 10..17 -> SlotGroup.MIDFIELD
            in 19..25 -> SlotGroup.ATTACK
            else -> SlotGroup.NONE
        }

    override fun toString(): String = "Slot($value)"

    companion object {
        val NONE = Slot(0)
        val UNUSED_SUBSTITUTE = Slot(-1)
        val PITCH_RANGE = 1..25
        val BENCH_RANGE = 26..36
    }
}

/**
 * Which line aggregate a slot feeds. Slots zero and eighteen feed none.
 */
@SpecRef("3.4")
enum class SlotGroup {
    KEEPER,
    DEFENCE,
    MIDFIELD,
    ATTACK,
    NONE,
}
