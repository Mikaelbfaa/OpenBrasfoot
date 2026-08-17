package org.openfoot.model

/**
 * Indices into a player's seven ability values, each in the range zero to one
 * hundred.
 *
 * Abilities are held in an IntArray rather than seven named fields because both
 * the slot weighting and the weekly growth distribution are table driven, so
 * index access keeps them as lookups instead of long when chains.
 */
@SpecRef("4.1")
object Attr {
    const val GOALKEEPING = 0
    const val PACE = 1
    const val TECHNIQUE = 2
    const val PASSING = 3
    const val TACKLING = 4
    const val PLAYMAKING = 5
    const val FINISHING = 6

    const val COUNT = 7

    /** Display order used by the squad table. */
    val displayOrder = intArrayOf(GOALKEEPING, TACKLING, PLAYMAKING, FINISHING, PACE, TECHNIQUE, PASSING)
}
