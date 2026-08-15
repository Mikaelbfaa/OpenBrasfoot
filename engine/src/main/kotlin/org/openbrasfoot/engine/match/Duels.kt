package org.openbrasfoot.engine.match

import org.openbrasfoot.model.HomeAdvantage
import org.openbrasfoot.model.Rng
import org.openbrasfoot.model.RuleSet
import org.openbrasfoot.model.SpecRef
import org.openbrasfoot.model.TeamSide
import kotlin.math.max

/** Who came out of the possession duel with the ball. */
@SpecRef("3.6a")
enum class DuelWinner {
    POSSESSOR,
    DEFENDER,
}

/** Whether the possessing side worked a shooting chance. */
@SpecRef("3.6b")
enum class ChanceOutcome {
    SHOT,
    NO_SHOT,
}

/**
 * Midfield against midfield, for the side currently on the ball.
 *
 * At equal strength the side in possession keeps it fifty five times in a
 * hundred, and at home on its own ticks that rises to about sixty one.
 *
 * The playing style dial of the original is read and discarded at this point,
 * so it has no effect whatsoever and is not modelled.
 */
@SpecRef("3.6a")
fun possessionDuel(
    possessorMidfield: Double,
    defenderMidfield: Double,
    advantage: HomeAdvantage,
    divisor: Double,
    rules: RuleSet,
    rng: Rng,
): DuelWinner {
    var keep = 1.0 + strengthDifference(possessorMidfield, defenderMidfield, divisor)
    val win = 1.0 + strengthDifference(defenderMidfield, possessorMidfield, divisor)

    if (advantage == HomeAdvantage.POSSESSOR_HOME) {
        keep += rules.homeDuelBonus
    }

    val floor = rules.duelWeightFloor
    val weights = doubleArrayOf(rules.possessionBaseWeights.first, rules.possessionBaseWeights.second)
    val multipliers = doubleArrayOf(max(keep, floor), max(win, floor))

    return if (weightedPick(weights, multipliers, rng) == 0) {
        DuelWinner.POSSESSOR
    } else {
        DuelWinner.DEFENDER
    }
}

/**
 * Attack against defence, deciding whether a chance becomes a shot.
 *
 * The steps are applied in the order the spec gives them, which matters. In
 * particular the floor runs after the anti exploit weights, so a defence with
 * no centre backs and one with a single centre back end up identical. Keeping
 * the literal order is deliberate: without the floor the single centre back
 * case would concede more shots than none at all, which is nonsense.
 *
 * The anti exploit branch only fires when a human managed club is in the match.
 * The original never punishes an artificial side for a broken back line.
 */
@SpecRef("3.6b")
fun chanceDuel(
    possessorAttack: Double,
    defenderDefence: Double,
    advantage: HomeAdvantage,
    humanInvolved: Boolean,
    defenderCentrebacks: Int,
    divisor: Double,
    rules: RuleSet,
    rng: Rng,
): ChanceOutcome {
    var attack = 1.0 + strengthDifference(possessorAttack, defenderDefence, divisor)
    var defend = 1.0 + strengthDifference(defenderDefence, possessorAttack, divisor)

    if (defenderDefence <= 0.0) {
        defend = rules.emptyLineDuelWeight
    }
    if (possessorAttack <= 0.0) {
        attack = rules.emptyLineDuelWeight
    }

    if (advantage == HomeAdvantage.POSSESSOR_HOME) {
        attack += rules.homeDuelBonus
    }

    if (humanInvolved) {
        when (defenderCentrebacks) {
            0 -> defend = rules.chanceNoCentrebackWeight
            1 -> defend = rules.chanceOneCentrebackWeight
        }
    }

    val floor = rules.duelWeightFloor
    val weights = doubleArrayOf(rules.chanceBaseWeights.first, rules.chanceBaseWeights.second)
    val multipliers = doubleArrayOf(max(attack, floor), max(defend, floor))

    return if (weightedPick(weights, multipliers, rng) == 0) {
        ChanceOutcome.SHOT
    } else {
        ChanceOutcome.NO_SHOT
    }
}

/**
 * Possession duel for a real pairing, resolving aggregates and divisor from the
 * setup. Returns the side that ends up with the ball.
 */
@SpecRef("3.6a")
fun possessionDuel(setup: MatchSetup, possessor: TeamSide, rng: Rng): TeamSide {
    val defender = possessor.opponent
    val winner = possessionDuel(
        possessorMidfield = lineAggregates(setup.side(possessor), setup.rules).midfield,
        defenderMidfield = lineAggregates(setup.side(defender), setup.rules).midfield,
        advantage = setup.advantageFor(possessor),
        divisor = differenceDivisor(setup.season, DuelKind.POSSESSION, setup.rules),
        rules = setup.rules,
        rng = rng,
    )
    return if (winner == DuelWinner.POSSESSOR) possessor else defender
}

/**
 * Chance duel for a real pairing.
 */
@SpecRef("3.6b")
fun chanceDuel(setup: MatchSetup, possessor: TeamSide, rng: Rng): ChanceOutcome {
    val defendingSide = setup.side(possessor.opponent)
    return chanceDuel(
        possessorAttack = lineAggregates(setup.side(possessor), setup.rules).attack,
        defenderDefence = lineAggregates(defendingSide, setup.rules).defence,
        advantage = setup.advantageFor(possessor),
        humanInvolved = setup.hasHumanSide,
        defenderCentrebacks = centrebackCount(defendingSide, setup.rules),
        divisor = differenceDivisor(setup.season, DuelKind.CHANCE, setup.rules),
        rules = setup.rules,
        rng = rng,
    )
}

/**
 * Centre backs are counted by the cell they occupy, not by natural position,
 * matching the rule that the engine groups by slot everywhere.
 */
@SpecRef("3.6b")
internal fun centrebackCount(side: MatchSide, rules: RuleSet): Int =
    side.lineup.count { it.slot.value in rules.centrebackSlots }
