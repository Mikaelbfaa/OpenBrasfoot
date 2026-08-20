package org.openfoot.engine.match

import org.openfoot.model.HomeAdvantage
import org.openfoot.model.Marking
import org.openfoot.model.PlayerId
import org.openfoot.model.Position
import org.openfoot.model.RuleSet
import org.openfoot.model.Slot
import org.openfoot.model.SpecRef
import org.openfoot.model.TeamSide
import org.openfoot.model.Trait

/**
 * One lineup entry as the match engine sees it.
 *
 * Deliberately not the world model player, which does not exist yet. The
 * formulas of sections 3.4 and 3.6 read only these nine things, so an adapter
 * will build this from a Player once worldgen lands and nothing here changes.
 *
 * Not a data class, because a data class's generated equals compares the
 * abilities array the same way Any.equals does, by reference, which is not
 * value equality either. equals and hashCode are hand written below instead,
 * comparing abilities with contentEquals, so that two players built from
 * identical inputs compare equal. That equality is what lets a shot's
 * shooter, now part of the log a match returns, be compared across two runs
 * of the same seed: two matches played from the same seed build separate
 * MatchPlayer instances for what is the same player, and only value equality
 * lets a whole match report compare equal to another played the same way.
 */
@SpecRef("3.4")
class MatchPlayer(
    val id: PlayerId,
    val slot: Slot,
    val naturalPosition: Position,
    @property:SpecRef("3.9") val age: Int,
    val strength: Int,
    val abilities: IntArray,
    val firstTrait: Trait,
    val secondTrait: Trait,
    val representsSideCountry: Boolean = false,
) {
    /** True when either of the player's two characteristics is the given one. */
    fun hasTrait(trait: Trait): Boolean = firstTrait == trait || secondTrait == trait

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MatchPlayer) return false
        return id == other.id &&
            slot == other.slot &&
            naturalPosition == other.naturalPosition &&
            age == other.age &&
            strength == other.strength &&
            abilities.contentEquals(other.abilities) &&
            firstTrait == other.firstTrait &&
            secondTrait == other.secondTrait &&
            representsSideCountry == other.representsSideCountry
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + slot.hashCode()
        result = 31 * result + naturalPosition.hashCode()
        result = 31 * result + age
        result = 31 * result + strength
        result = 31 * result + abilities.contentHashCode()
        result = 31 * result + firstTrait.hashCode()
        result = 31 * result + secondTrait.hashCode()
        result = 31 * result + representsSideCountry.hashCode()
        return result
    }
}

/**
 * One team for one match.
 *
 * The order of the lineup list is load bearing. Section 3.4 walks it in order
 * and takes the first N players that qualify for a line, not the best N, so
 * this list must never be sorted.
 *
 * Bench entries may sit in the same list. Slots twenty six to thirty six fall
 * outside every line range, so the aggregates ignore them without special
 * handling, and shooter selection filters them explicitly.
 */
@SpecRef("3.4")
class MatchSide(
    val lineup: List<MatchPlayer>,
    val marking: Marking,
    val context: StrengthContext,
    val isHumanManaged: Boolean = false,
) {
    val reputation: Int get() = context.sideReputation

    val isHome: Boolean get() = context.isHomeSide

    private val countrymanContext = context.copy(playerRepresentsSideCountry = true)

    /**
     * The player's rating for this match on the zero to ten scale.
     *
     * Recomputed on every call, matching the original, which keeps no cache.
     */
    @SpecRef("3.3")
    fun ratingOf(player: MatchPlayer): Double = effectiveStrength(
        strength = player.strength,
        attrs = player.abilities,
        naturalPosition = player.naturalPosition,
        slot = player.slot,
        context = if (player.representsSideCountry) countrymanContext else context,
    )
}

/**
 * The two sides plus everything a tick needs that does not change during the
 * match.
 */
@SpecRef("3.5")
class MatchSetup(
    val home: MatchSide,
    val away: MatchSide,
    val season: Int,
    val rules: RuleSet,
) {
    val isNeutralGround: Boolean get() = home.context.kind.isNeutralGround

    /**
     * The anti exploit rules of sections 3.6b and 3.6c only fire when a human
     * managed club is involved, on either side.
     */
    @SpecRef("3.6b")
    val hasHumanSide: Boolean get() = home.isHumanManaged || away.isHumanManaged

    fun side(team: TeamSide): MatchSide = if (team == TeamSide.HOME) home else away

    fun advantageFor(possessor: TeamSide): HomeAdvantage =
        HomeAdvantage.of(possessor, isNeutralGround)
}
