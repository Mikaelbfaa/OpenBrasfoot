package org.openbrasfoot.model

/**
 * The tackling hardness dial, and the only tactical setting in the original
 * that has any mechanical effect at all.
 *
 * It does three things: a tiny bonus to the midfield total, a large change to
 * the booking rate, and an assist weight bonus for fullbacks. The other three
 * dials of the original are read and discarded, so they are not modelled here.
 */
@SpecRef("3.12")
enum class Marking {
    LIGHT,
    HEAVY,
    VERY_HEAVY,
    ;

    companion object {
        fun ofOrdinal(value: Int): Marking = entries.getOrNull(value)
            ?: throw IllegalArgumentException("unknown marking ordinal $value")
    }
}
