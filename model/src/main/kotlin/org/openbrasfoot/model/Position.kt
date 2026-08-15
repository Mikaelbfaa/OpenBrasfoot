package org.openbrasfoot.model

/**
 * Natural playing position. The ordinal matches the value stored in the
 * original data files, which the importer relies on.
 */
@SpecRef("FORMAT-SPEC, jogador")
enum class Position {
    GOALKEEPER,
    FULLBACK,
    CENTREBACK,
    MIDFIELDER,
    FORWARD,
    ;

    companion object {
        fun ofOrdinal(value: Int): Position = entries.getOrNull(value)
            ?: throw IllegalArgumentException("unknown position ordinal $value")
    }
}

/**
 * Preferred side of the pitch. Carries no strength penalty at all, it only
 * influences automatic lineup selection.
 */
@SpecRef("5.3")
enum class Side {
    RIGHT,
    LEFT,
    ;

    companion object {
        fun ofOrdinal(value: Int): Side = entries.getOrNull(value)
            ?: throw IllegalArgumentException("unknown side ordinal $value")
    }
}
