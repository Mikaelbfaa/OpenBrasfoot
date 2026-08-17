package org.openfoot.model

/**
 * Which of the two teams in a match is meant.
 */
enum class TeamSide {
    HOME,
    AWAY,
    ;

    val opponent: TeamSide get() = if (this == HOME) AWAY else HOME
}

/**
 * Whether home advantage applies to the side currently on the ball.
 *
 * A single value rather than a pair of booleans for neutral ground and for who
 * is at home, because those two can be set inconsistently and this cannot.
 * NONE covers both sides on neutral ground, which is the club world cup and
 * national team competitions.
 */
@SpecRef("3.11")
enum class HomeAdvantage {
    NONE,
    POSSESSOR_HOME,
    POSSESSOR_AWAY,
    ;

    companion object {
        /**
         * Neutral ground removes the advantage for both sides.
         */
        fun of(possessor: TeamSide, neutralGround: Boolean): HomeAdvantage = when {
            neutralGround -> NONE
            possessor == TeamSide.HOME -> POSSESSOR_HOME
            else -> POSSESSOR_AWAY
        }
    }
}
