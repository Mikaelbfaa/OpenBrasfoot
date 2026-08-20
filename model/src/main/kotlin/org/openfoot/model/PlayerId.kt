package org.openfoot.model

/**
 * Which player this is, for the duration of one match.
 *
 * Energy, bookings and substitutions are all kept as maps from a player to a
 * number, and a map needs a key. The cell cannot be that key, because a cell
 * changes when a substitute comes on and the original leaves an unused
 * substitute on minus one. The name cannot be that key either, because two
 * players in one squad may share one.
 *
 * The number is the player's index into the squad the lineup was picked from,
 * which makes it stable for as long as that squad is, and cheap.
 */
@JvmInline
@SpecRef("3.2")
value class PlayerId(val value: Int) {

    override fun toString(): String = "PlayerId($value)"

    companion object {
        /** A player who has not been given an identity, outside any squad index. */
        val UNASSIGNED = PlayerId(-1)
    }
}
