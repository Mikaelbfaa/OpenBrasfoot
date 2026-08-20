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
 * Section 5.4 writes out one of these chains, the one for the keeper cell:
 * keeper, then centre back, then fullback, then midfielder, then forward. That
 * is the five positions in order of distance along the line the game arranges
 * them on, so the other four chains are the same rule read from a different
 * starting point, nearest position first.
 *
 * Two details the spec leaves open are settled here. Positions equally far
 * away are tried from the defensive end first, because squads carry more
 * defenders than forwards. And the keeper goes last in every outfield chain
 * rather than in his place on the line, because section 5.3 charges a non
 * keeper in goal an extra collapse of the keeper aggregate on top of the flat
 * halving every out of position player pays, so emptying the goal is the one
 * improvisation that costs more than the others.
 *
 * Distance is all that separates these, since section 5.3 charges the same
 * flat half to a fullback in midfield as to a keeper up front.
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
 * The original runs no legality check anywhere in its lineup handling and
 * fields eleven whatever the squad looks like, so a squad of eleven keepers
 * produces eleven players on the pitch rather than an exception. Fewer than
 * eleven come back only when fewer than eleven can play.
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
    val availabilities = squad.mapIndexed { index, player -> availability.of(index, player) }
    val pool = squad.indices
        .filter { availabilities[it].canPlay }
        .sortedWith(
            compareByDescending<Int> { squad[it].strength }
                .thenByDescending { availabilities[it].energy },
        )

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
 * The relaxed search of section 5.4 step 3 for one cell.
 *
 * The outer loop walks the position cascade and the inner one relaxes side and
 * then style, so a player of the right position playing on the wrong flank is
 * preferred to a player of another position playing on the right one.
 *
 * The inner loop is where defect 7 of section 3.15 lives. Three passes are
 * described, exact, then side ignored, then side and style ignored, and the
 * loop bound of the original never reaches the last of them. The count is a
 * rule set field: under the classic rules a cell that cannot find its own sub
 * role gives up on the position entirely and cascades, which is how a centre
 * back ends up as a holding midfielder while a playmaker sits.
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
 * given level of relaxation.
 */
@SpecRef("5.4")
private fun fits(slot: Slot, player: Player, pass: Int): Boolean {
    val sideOk = slot.requiredSide == null || slot.requiredSide == player.side
    val styleOk = slot.requiredStyle == null || slot.requiredStyle == player.style
    return when (pass) {
        0 -> sideOk && styleOk
        1 -> styleOk
        else -> true
    }
}
