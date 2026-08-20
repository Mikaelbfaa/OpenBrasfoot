package org.openfoot.model

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
 *
 * The three requiredX properties below are the one table of section 3.2 read
 * three ways: what position a cell asks for, what side and what sub role. The
 * relaxed search of section 5.4 reads all three, in that order of stubbornness,
 * and they live together here so that no part of the table can be changed
 * without its siblings in view.
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

    /**
     * The side of the pitch this cell belongs to, or null for a cell in the
     * middle.
     *
     * Section 3.2 names a side for exactly three pairs of cells: the fullbacks
     * 2 and 9 are written "direito / esquerdo", and the wing backs 10 and 17
     * and the wingers 18 and 25 are the two other pairs the same table sets
     * apart from their neighbours. The lower number of each pair is the right,
     * following the order the fullback row spells out. Every other cell is
     * central and takes either side without preference.
     *
     * A wrong side costs nothing at all in section 5.3. It is only ever a
     * preference in the lineup, which is why it is the first thing the relaxed
     * search of section 5.4 gives up on.
     */
    @SpecRef("3.2")
    val requiredSide: Side?
        get() = when (value) {
            2, 10, 18 -> Side.RIGHT
            9, 17, 25 -> Side.LEFT
            else -> null
        }

    /**
     * The sub role this cell asks for, or null for a cell that asks for none.
     *
     * Read straight off the table of section 3.2 through the derivation of
     * section 4.3. The holding cells 11 to 13 want a defensive midfielder and
     * the attacking cells 14 to 16 want an offensive one; the wingers 18 and
     * 25 want the winger reading of a forward and the central cells 19 to 24
     * want the centre forward reading; the wing backs 10 and 17, which section
     * 3.2 calls alas and which sit in the midfield range while demanding a
     * fullback, want the offensive reading of one.
     *
     * The keeper and the six centre back cells ask for the defensive reading.
     * That is free at their own position, where it is the only reading
     * available, and it matters during the cascade of section 5.4, where it
     * steers a centre back cell towards a defensive fullback or a holding
     * midfielder rather than a winger.
     *
     * Cells 2 and 9 ask for no sub role. The table names them only by their
     * side, unlike every other pair of cells it lists, and the alternative
     * reading does not survive contact with the game: the derivation of
     * section 4.3 makes any fullback with pace or crossing offensive, so
     * demanding the defensive reading at 2 and 9 would stop most fullbacks in
     * the game from being picked for the cells they are named after.
     */
    @SpecRef("3.2")
    val requiredStyle: PlayerStyle?
        get() = when (value) {
            2, 9 -> null
            1, in 3..8 -> PlayerStyle.DEFENSIVE
            10, 17 -> PlayerStyle.OFFENSIVE
            in 11..13 -> PlayerStyle.DEFENSIVE
            in 14..16 -> PlayerStyle.OFFENSIVE
            18, 25 -> PlayerStyle.WINGER
            in 19..24 -> PlayerStyle.OFFENSIVE
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
 * Which line a cell belongs to under the classic rules, where slots zero and
 * eighteen belong to none.
 *
 * This is a convenience for display and for the importer. Engine formulas must
 * never use it: they take their line ranges from the rule set, because slot
 * eighteen belonging to no line is a defect of the original that the modern
 * rules repair. Hard coding it here would make that repair impossible.
 */
@SpecRef("3.4")
enum class SlotGroup {
    KEEPER,
    DEFENCE,
    MIDFIELD,
    ATTACK,
    NONE,
}
