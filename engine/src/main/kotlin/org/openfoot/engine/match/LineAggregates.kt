package org.openfoot.engine.match

import org.openfoot.model.RuleSet
import org.openfoot.model.Slot
import org.openfoot.model.SpecRef
import org.openfoot.model.bfRound

/**
 * The four numbers a tick works from.
 */
@SpecRef("3.4")
data class LineAggregates(
    val keeper: Double,
    val defence: Double,
    val midfield: Double,
    val attack: Double,
)

/**
 * Running total of the ratings taken from one line.
 */
internal data class LineTally(val total: Double, val count: Int)

/**
 * Recomputes all four line ratings from the current lineup.
 *
 * There is no cache anywhere, matching the original, which recomputes on every
 * use. Two properties of this function drive the whole balance of the game and
 * must not be tidied up:
 *
 * The divisors are fixed constants rather than the number of players found, so
 * fielding four midfielders instead of five costs a fifth of the midfield
 * rating, and a lone striker gives an attack rating of his own strength divided
 * by three.
 *
 * Players are taken in lineup list order, first N that qualify, not the best N.
 *
 * Line membership comes from the rule set slot ranges and never from
 * Slot.group, otherwise the classic defect where slot eighteen belongs to no
 * line would be hard coded and the modern rules could not repair it.
 */
@SpecRef("3.4")
fun lineAggregates(side: MatchSide, rules: RuleSet): LineAggregates = LineAggregates(
    keeper = keeperAggregate(side, rules),
    defence = defenceAggregate(side, rules),
    midfield = midfieldAggregate(side, rules),
    attack = attackAggregate(side, rules),
)

@SpecRef("3.4")
internal fun defenceAggregate(side: MatchSide, rules: RuleSet): Double {
    val tally = tally(side, rules.defenceSlots, rules.defenceTake)
    if (tally.count < rules.defenceMinimum) {
        return rules.shorthandedLineRating
    }
    return tally.total / rules.defenceDivisor
}

/**
 * The only line with a tactical input. The marking bonus is added to the total
 * before the fixed divisor, which is what makes it worth a hundredth of a point
 * on the zero to ten scale rather than anything meaningful.
 *
 * A shorthanded midfield returns the degenerate rating without the bonus.
 */
@SpecRef("3.4")
internal fun midfieldAggregate(side: MatchSide, rules: RuleSet): Double {
    val tally = tally(side, rules.midfieldSlots, rules.midfieldTake)
    if (tally.count < rules.midfieldMinimum) {
        return rules.shorthandedLineRating
    }
    return (tally.total + rules.markingBonus(side.marking)) / rules.midfieldDivisor
}

@SpecRef("3.4")
internal fun attackAggregate(side: MatchSide, rules: RuleSet): Double {
    val tally = tally(side, rules.attackSlots, rules.attackTake)
    if (tally.count == 0) {
        return rules.emptyAttackRating
    }
    return tally.total / rules.attackDivisor
}

/**
 * The keeper rating, which is the one place a line is a single player.
 *
 * An outfield player in goal is punished twice. His own rating has already been
 * halved for being out of position, and then the whole line rating is scaled
 * and rounded to a whole number on the zero to ten scale. A strength seventy
 * outfielder ends at one against a real keeper's seven, and a weaker one can
 * legitimately reach zero, which the shot resolution floor then absorbs.
 */
@SpecRef("3.4")
internal fun keeperAggregate(side: MatchSide, rules: RuleSet): Double {
    val keeper = side.lineup.firstOrNull { it.slot.value == rules.keeperSlot }
        ?: return rules.missingKeeperRating
    val rating = side.ratingOf(keeper)
    if (!isOutOfPosition(keeper.naturalPosition, Slot(rules.keeperSlot))) {
        return rating
    }
    return bfRound(rating * rules.keeperOutOfPositionFactor).toDouble()
}

/**
 * Walks the lineup in list order and sums the ratings of the first take players
 * whose cell falls in the given range.
 */
@SpecRef("3.4")
internal fun tally(side: MatchSide, slots: IntRange, take: Int): LineTally {
    var total = 0.0
    var count = 0
    for (player in side.lineup) {
        if (count == take) {
            break
        }
        if (player.slot.value in slots) {
            total += side.ratingOf(player)
            count++
        }
    }
    return LineTally(total, count)
}
