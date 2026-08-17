package org.openfoot.engine.world

import org.openfoot.model.SpecRef

/**
 * The two quality numbers every generated player of a club derives from.
 *
 * The base feeds the strength formula of section 4.4 directly. The band is what
 * section 4.2 calls B, the quantity almost every secondary attribute is built
 * from. Both come from the same lookup, which is why they travel together.
 *
 * They are the reason a club's players look like its club: only a player's
 * primary attribute derives from his own strength, and everything else derives
 * from these two.
 */
@SpecRef("4.4")
data class GenerationBands(
    @property:SpecRef("4.4") val strengthBase: Int,
    @property:SpecRef("4.2") val abilityBand: Int,
)

/**
 * Club level arithmetic shared by the strength and attribute formulas.
 */
object ClubBands {

    /**
     * Expands the stored club level onto the scale the formulas use.
     *
     * Levels up to fifteen pass through untouched. Above that the mapping
     * accelerates, so the gap between a very good club and an elite one widens
     * faster than the stored level suggests.
     */
    @SpecRef("4.4")
    fun mappedLevel(level: Int): Int = when {
        level <= FLAT_LEVEL_CEILING -> level
        level == 16 -> 17
        level == 17 -> 18
        level == 18 -> 19
        level == 19 -> 21
        else -> level + 5
    }

    /**
     * The club quality seed that section 4.2 calls A.
     *
     * The spec writes this as the mapped level, minus four when it exceeds
     * four. Club levels run from six to twenty, and the mapping is the identity
     * up to fifteen, so the mapped level is never four or less and the guard
     * never fires. The subtraction is unconditional in practice. See
     * OPEN-QUESTIONS item 15 for why the guard is kept anyway.
     */
    @SpecRef("4.2")
    fun qualitySeed(level: Int): Int {
        val mapped = mappedLevel(level)
        return if (mapped > QUALITY_SEED_GUARD) mapped - QUALITY_SEED_OFFSET else mapped
    }

    /**
     * The bands for a squad: by reputation for a national team, by division for
     * everyone else.
     *
     * Only a national team reads reputation. A club whose division is unknown
     * lands on the weakest pair rather than on the reputation path, because a
     * missing division is not evidence of standing and treating it as one would
     * quietly promote every such club.
     *
     * A division outside the top three lands on the weakest pair too, which
     * covers the fourth tier and the division zero of a country with no league
     * pyramid alike. Reputation below four is indistinguishable here.
     */
    @SpecRef("4.4")
    fun bands(division: Int?, reputation: Int, nationalTeam: Boolean = false): GenerationBands =
        if (nationalTeam) nationalTeamBands(reputation) else leagueBands(division)

    @SpecRef("4.4")
    private fun leagueBands(division: Int?): GenerationBands = when (division) {
        1 -> GenerationBands(strengthBase = 20, abilityBand = 7)
        2 -> GenerationBands(strengthBase = 15, abilityBand = 3)
        3 -> GenerationBands(strengthBase = 5, abilityBand = 1)
        else -> GenerationBands(strengthBase = 1, abilityBand = 1)
    }

    @SpecRef("4.4")
    private fun nationalTeamBands(reputation: Int): GenerationBands = when (reputation) {
        5 -> GenerationBands(strengthBase = 22, abilityBand = 7)
        4 -> GenerationBands(strengthBase = 15, abilityBand = 4)
        else -> GenerationBands(strengthBase = 5, abilityBand = 1)
    }

    /** Levels at or below this map to themselves. */
    @SpecRef("4.4")
    private const val FLAT_LEVEL_CEILING = 15

    @SpecRef("4.2")
    private const val QUALITY_SEED_GUARD = 4

    @SpecRef("4.2")
    private const val QUALITY_SEED_OFFSET = 4
}
