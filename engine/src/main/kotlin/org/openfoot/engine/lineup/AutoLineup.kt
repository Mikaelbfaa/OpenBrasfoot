package org.openfoot.engine.lineup

import org.openfoot.engine.match.MatchPlayer
import org.openfoot.engine.world.Player
import org.openfoot.engine.world.PlayerStyle
import org.openfoot.engine.world.inSlot
import org.openfoot.model.PlayerId
import org.openfoot.model.Position
import org.openfoot.model.RuleSet
import org.openfoot.model.Side
import org.openfoot.model.Slot
import org.openfoot.model.SpecRef

/**
 * The side of the pitch a cell belongs to, or null for a cell in the middle.
 *
 * Section 3.2 names a side for exactly three pairs of cells: the fullbacks 2
 * and 9 are written "direito / esquerdo", and the wing backs 10 and 17 and the
 * wingers 18 and 25 are the two other pairs the same table sets apart from
 * their neighbours. The lower number of each pair is the right, following the
 * order the fullback row spells out. Every other cell is central and takes
 * either side without preference.
 *
 * A wrong side costs nothing at all in section 5.3. It is only ever a
 * preference in the lineup, which is why it is the first thing the search
 * below gives up on.
 */
@SpecRef("3.2")
internal val Slot.requiredSide: Side?
    get() = when (value) {
        2, 10, 18 -> Side.RIGHT
        9, 17, 25 -> Side.LEFT
        else -> null
    }

/**
 * The sub role a cell asks for, or null for a cell that asks for none.
 *
 * Read straight off the table of section 3.2 through the derivation of section
 * 4.3. The holding cells 11 to 13 want a defensive midfielder and the
 * attacking cells 14 to 16 want an offensive one; the wingers 18 and 25 want
 * the winger reading of a forward and the central cells 19 to 24 want the
 * centre forward reading; the wing backs 10 and 17, which section 3.2 calls
 * alas and which sit in the midfield range while demanding a fullback, want
 * the offensive reading of one.
 *
 * The keeper and the six centre back cells ask for the defensive reading. That
 * is free at their own position, where it is the only reading available, and
 * it matters during the cascade, where it steers a centre back cell towards a
 * defensive fullback or a holding midfielder rather than a winger.
 *
 * Cells 2 and 9 ask for no sub role. The table names them only by their side,
 * unlike every other pair of cells it lists, and the alternative reading does
 * not survive contact with the game: the derivation of section 4.3 makes any
 * fullback with pace or crossing offensive, so demanding the defensive reading
 * at 2 and 9 would stop most fullbacks in the game from being picked for the
 * cells they are named after.
 */
@SpecRef("3.2")
internal val Slot.requiredStyle: PlayerStyle?
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
internal val POSITION_CASCADE: Map<Position, List<Position>> = mapOf(
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
 * Picks the eleven a squad fields in a formation, the way section 5.4 does it.
 *
 * The pool is filtered, sorted by strength descending and then energy
 * descending, and nothing else is ever consulted again. Each cell of the
 * formation is then filled, in the order the formation lists its cells, by the
 * first player left in that pool who fits. Because the formation lists name
 * the forwards before the defenders, the attack picks from the top of the pool
 * and the defence takes what survives, which is the AI behaviour section 5.4
 * insists a reimplementation reproduce.
 *
 * A cell that finds nobody at all takes the strongest player left regardless.
 * Section 5.4 does not say what happens when a chain runs out, and this is the
 * reading recorded as item 35 of OPEN-QUESTIONS: the bench of section 5.4 step
 * 4 is a fixed eleven and the aggregates of section 3.4 have no notion of an
 * empty pitch cell, so a side that fielded ten would be visible everywhere and
 * is described nowhere. Fewer than eleven come back only when fewer than
 * eleven can play.
 *
 * The identity handed to each player is his index in the squad passed in,
 * which is what keeps it stable for as long as that squad is.
 */
@SpecRef("5.4")
fun fillEleven(
    squad: List<Player>,
    formation: Formation,
    rules: RuleSet,
    availability: Availability = Availability.FULL_SQUAD,
): List<MatchPlayer> {
    val pool = sortedPool(squad, availability)
    val taken = BooleanArray(squad.size)
    val eleven = mutableListOf<MatchPlayer>()
    for (slot in formation.slots) {
        val chosen = chooseFor(slot, squad, pool, taken, rules) ?: continue
        taken[chosen] = true
        eleven += squad[chosen].inSlot(slot, PlayerId(chosen))
    }
    return eleven
}

/**
 * The pool of section 5.4 step 1 and 2: not injured, not suspended, sorted by
 * strength descending and then energy descending, and nothing else. Shared by
 * the eleven and the bench, because both are drawn from the same ordering and
 * a second copy of it would be a second place for the two to drift apart.
 */
@SpecRef("5.4")
private fun sortedPool(squad: List<Player>, availability: Availability): List<Int> {
    val availabilities = squad.mapIndexed { index, player -> availability.of(index, player) }
    return squad.indices
        .filter { availabilities[it].canPlay }
        .sortedWith(
            compareByDescending<Int> { squad[it].strength }
                .thenByDescending { availabilities[it].energy },
        )
}

/**
 * The eleven a squad fields together with who sits behind them, which is what
 * the rest of the engine asks for: a match never needs one without the other.
 */
@SpecRef("5.4")
data class MatchdaySquad(
    val onPitch: List<MatchPlayer>,
    val bench: List<MatchPlayer>,
)

/**
 * Names the eleven of section 5.4 step 3 and then the bench of step 4, from
 * whoever the eleven left behind.
 *
 * The bench template, 1, 1, 2, 4, 4, 12, 15, 15, 20, 20, 23, is read as model
 * cells, not as cells a substitute stands in: cell 1 asks for the goalkeeper
 * reading, cell 2 for a fullback on the right, cell 4 for a centre back, cell
 * 12 for a holding midfielder, cell 15 for an attacking one and cell 20 and 23
 * for a centre forward, twice and once. Each entry runs through the same
 * position cascade and side/style relaxation that fills the eleven, so a bench
 * place that finds nobody of its own kind cascades exactly as a pitch cell
 * would, right down to the catch all that seats the strongest man left over
 * regardless of fit.
 *
 * A squad too small to fill every bench place benches fewer rather than
 * failing: once every remaining player has been used, the catch all itself has
 * nobody left to hand back, and the template stops rather than repeating a man
 * already on the pitch or already on the bench.
 *
 * Every bench entry carries Slot.UNUSED_SUBSTITUTE, which is section 3.2's
 * minus one for a substitute who has not come on, never the template cell that
 * chose him. The template cell is only ever a question asked of the pool, not
 * an answer recorded on the player.
 */
@SpecRef("5.4")
fun autoLineup(
    squad: List<Player>,
    formation: Formation,
    rules: RuleSet,
    availability: Availability = Availability.FULL_SQUAD,
): MatchdaySquad {
    val onPitch = fillEleven(squad, formation, rules, availability)

    val pool = sortedPool(squad, availability)
    val taken = BooleanArray(squad.size)
    for (player in onPitch) {
        taken[player.id.value] = true
    }

    val bench = mutableListOf<MatchPlayer>()
    for (cell in rules.benchTemplate) {
        val chosen = chooseFor(Slot(cell), squad, pool, taken, rules) ?: break
        taken[chosen] = true
        bench += squad[chosen].inSlot(Slot.UNUSED_SUBSTITUTE, PlayerId(chosen))
    }

    return MatchdaySquad(onPitch, bench)
}

/**
 * The relaxed search of section 5.4 step 3 for one cell.
 *
 * The outer loop walks the position cascade and the inner one relaxes side and
 * then style, so a player of the right position playing on the wrong flank is
 * preferred to a player of another position playing on the right one.
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
private fun chooseFor(
    slot: Slot,
    squad: List<Player>,
    pool: List<Int>,
    taken: BooleanArray,
    rules: RuleSet,
): Int? {
    val required = slot.requiredPosition
    if (required != null) {
        for (position in POSITION_CASCADE.getValue(required)) {
            for (pass in 0 until rules.lineupRelaxationPasses) {
                val found = pool.firstOrNull { index ->
                    !taken[index] && squad[index].position == position && fits(slot, squad[index], pass)
                }
                if (found != null) {
                    return found
                }
            }
        }
    }
    return pool.firstOrNull { !taken[it] }
}

/**
 * Whether a player satisfies what the cell asks beyond the position, at the
 * given level of relaxation. The three levels and their order are section
 * 3.2: exact, then side ignored, then side and style ignored.
 */
@SpecRef("3.2")
private fun fits(slot: Slot, player: Player, pass: Int): Boolean {
    val sideOk = slot.requiredSide == null || slot.requiredSide == player.side
    val styleOk = slot.requiredStyle == null || slot.requiredStyle == player.style
    return when (pass) {
        0 -> sideOk && styleOk
        1 -> styleOk
        else -> true
    }
}
