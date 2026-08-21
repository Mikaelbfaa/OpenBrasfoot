package org.openfoot.model

/**
 * The order in which a cell gives up on the position it asked for.
 *
 * Section 5.4 writes out one of these five chains, the one for the keeper
 * cell: keeper, then centre back, then fullback, then midfielder, then
 * forward. The other four are not written anywhere, and two different rules
 * reproduce the written one exactly, so this is a choice rather than a
 * reading. Nearest first along the line the five positions sit on is the one
 * implemented; the other candidate is the written order itself, walked from
 * the top with the position the cell asked for lifted to the front. They
 * disagree from midfield, where nearest first tries fullback and forward
 * before centre back and the other tries centre back first. Item 33 of
 * OPEN-QUESTIONS carries both and the reasoning.
 *
 * Positions equally far away are tried from the defensive end first, because
 * squads carry more defenders than forwards.
 *
 * The keeper goes last in every outfield chain instead of where distance would
 * put him. That has no support in the spec and is a preference: it only ever
 * bites when the squad has a spare keeper, and it says a reserve keeper is the
 * last man the AI improvises with. What little argument there is comes from
 * section 3.3, whose weight table gives the goalkeeping attribute a share only
 * in cell 1, so with the individual ability option turned on a keeper in an
 * outfield cell loses his best attribute outright. With that option off the
 * choice is arbitrary. The preference is not idle: item 32 of OPEN-QUESTIONS
 * carries a measured lineup where a centre back cell reaches the end of this
 * chain and the reserve keeper plays there.
 *
 * Nothing here changes how well a player performs. Section 5.3 charges the
 * same flat half to a fullback in midfield as to a keeper up front, so a chain
 * decides who plays and never how well.
 */
@SpecRef("5.4")
val POSITION_CASCADE: Map<Position, List<Position>> = mapOf(
    Position.GOALKEEPER to listOf(
        Position.GOALKEEPER,
        Position.CENTREBACK,
        Position.FULLBACK,
        Position.MIDFIELDER,
        Position.FORWARD,
    ),
    Position.CENTREBACK to listOf(
        Position.CENTREBACK,
        Position.FULLBACK,
        Position.MIDFIELDER,
        Position.FORWARD,
        Position.GOALKEEPER,
    ),
    Position.FULLBACK to listOf(
        Position.FULLBACK,
        Position.CENTREBACK,
        Position.MIDFIELDER,
        Position.FORWARD,
        Position.GOALKEEPER,
    ),
    Position.MIDFIELDER to listOf(
        Position.MIDFIELDER,
        Position.FULLBACK,
        Position.FORWARD,
        Position.CENTREBACK,
        Position.GOALKEEPER,
    ),
    Position.FORWARD to listOf(
        Position.FORWARD,
        Position.MIDFIELDER,
        Position.FULLBACK,
        Position.CENTREBACK,
        Position.GOALKEEPER,
    ),
)

/**
 * Whether a player satisfies what the cell asks beyond the position, at the
 * given level of relaxation. The three levels and their order are section
 * 3.2: exact, then side ignored, then side and style ignored.
 *
 * The two tables read here, Slot.requiredSide and Slot.requiredStyle, sit on
 * Slot in the model module next to Slot.requiredPosition. They are three
 * columns of one table of section 3.2 and they are kept together so that a
 * change to one row is reviewed against its siblings, and so that all three
 * are covered by one test class rather than half of them living out here
 * where the coverage was not.
 */
@SpecRef("3.2")
fun fits(slot: Slot, candidate: SlotCandidate, pass: Int): Boolean {
    val sideOk = slot.requiredSide == null || slot.requiredSide == candidate.side
    val styleOk = slot.requiredStyle == null || slot.requiredStyle == candidate.style
    return when (pass) {
        0 -> sideOk && styleOk
        1 -> styleOk
        else -> true
    }
}

/**
 * What a cell of the twenty five slot grid asks of somebody who might fill it,
 * and nothing else.
 *
 * Three properties, because that is what section 3.2's table has columns for.
 * Strength is deliberately absent: the caller decides the order candidates are
 * offered in, and the search below always takes the first that fits, so the
 * ordering rule of section 5.4 step 2 stays with the caller that knows it.
 */
@SpecRef("3.2")
interface SlotCandidate {
    val position: Position
    val side: Side
    val style: PlayerStyle
}

/**
 * The relaxed search of section 5.4 step 3 for one cell.
 *
 * The outer loop walks the position cascade and the inner one relaxes side and
 * then style, so a player of the right position on the wrong flank is
 * preferred to a player of another position on the right one. Candidates are
 * taken in the order given and the first that fits wins, so the caller's
 * ordering is the tie break and there is none of its own.
 *
 * A cell that exhausts the cascade takes whoever is first regardless of fit.
 * Item 35 of OPEN-QUESTIONS carries the reasoning: section 3.4 has no notion
 * of an empty pitch cell, so a side that fielded ten would be visible
 * everywhere and is described nowhere.
 *
 * The inner loop is where defect 7 of section 3.15 lives. Section 3.2
 * describes three passes, exact, then side ignored, then side and style
 * ignored, and the loop bound of the original never reaches the last of them.
 * The count is a rule set field: under the classic rules a cell that cannot
 * find its own sub role gives up on the position entirely and cascades, which
 * is how a centre back ends up as a holding midfielder while a playmaker sits.
 * Item 32 of OPEN-QUESTIONS carries the competing reading, under which the
 * unreachable step is the last position of each cascade instead.
 */
@SpecRef("5.4")
fun <T : SlotCandidate> chooseCandidate(slot: Slot, candidates: List<T>, rules: RuleSet): T? {
    val required = slot.requiredPosition
    if (required != null) {
        for (position in POSITION_CASCADE.getValue(required)) {
            for (pass in 0 until rules.lineupRelaxationPasses) {
                val found = candidates.firstOrNull { it.position == position && fits(slot, it, pass) }
                if (found != null) {
                    return found
                }
            }
        }
    }
    return candidates.firstOrNull()
}
