package org.openbrasfoot.model

/**
 * The sixteen competition categories. The ordinal matches the value stored in
 * the original data files.
 *
 * Club world cup and national team tournaments are the only ones played on
 * neutral ground, which switches home advantage off entirely.
 */
@SpecRef("1.1")
enum class CompetitionKind {
    FRIENDLY,
    NATIONAL_LEAGUE,
    NATIONAL_CUP,
    STATE,
    CONTINENTAL_PRIMARY,
    CLUB_WORLD_CUP,
    CONTINENTAL_SECONDARY,
    NATIONAL_TEAM,
    RECOPA,
    QUALIFIERS,
    REGIONAL,
    SUPER_CUP,
    CONFERENCE,
    FINALISSIMA,
    NATIONS_LEAGUE,
    FRIENDLY_TOURNAMENT,
    ;

    /** Neutral ground means no home advantage anywhere in the match. */
    @SpecRef("3.11")
    val isNeutralGround: Boolean
        get() = this == CLUB_WORLD_CUP || this == NATIONAL_TEAM

    companion object {
        fun ofOrdinal(value: Int): CompetitionKind = entries.getOrNull(value)
            ?: throw IllegalArgumentException("unknown competition kind ordinal $value")
    }
}
