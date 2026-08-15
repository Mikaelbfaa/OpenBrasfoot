package org.openbrasfoot.model

/**
 * The fourteen player characteristics. Every player has exactly two of them.
 * The ordinal matches the value stored in the original data files.
 *
 * Indices zero to three belong to goalkeepers and four to thirteen to outfield
 * players, but nothing enforces that: the data decides.
 */
@SpecRef("FORMAT-SPEC, caracteristicas")
enum class Trait {
    POSITIONING,
    PENALTY_SAVING,
    REFLEXES,
    RUSHING_OUT,
    PLAYMAKING,
    HEADING,
    CROSSING,
    TACKLING,
    DRIBBLING,
    FINISHING,
    MARKING,
    PASSING,
    STAMINA,
    PACE,
    ;

    /** True for the four characteristics only goalkeepers are given. */
    val isGoalkeeping: Boolean get() = ordinal <= RUSHING_OUT.ordinal

    companion object {
        fun ofOrdinal(value: Int): Trait = entries.getOrNull(value)
            ?: throw IllegalArgumentException("unknown trait ordinal $value")
    }
}
