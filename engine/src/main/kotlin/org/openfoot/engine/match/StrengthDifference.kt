package org.openfoot.engine.match

import org.openfoot.model.RuleSet
import org.openfoot.model.SpecRef

/**
 * Which of the three duels a strength comparison belongs to. They share a
 * divisor early on and diverge from the compression season.
 */
@SpecRef("3.5")
enum class DuelKind {
    POSSESSION,
    CHANCE,
    SHOT,
}

/**
 * How much a difference in line ratings is worth in a given season.
 *
 * From the compression season onward the divisors grow, so the same gap in
 * strength buys less. The game levels out as a career goes on, which is a
 * deliberate property of the original and not an accident.
 *
 * Seasons are counted from one.
 */
@SpecRef("3.5")
fun differenceDivisor(season: Int, duel: DuelKind, rules: RuleSet): Double {
    if (season < rules.compressionFirstSeason) {
        return rules.differenceDivisor
    }
    return if (duel == DuelKind.SHOT) {
        rules.lateShotDifferenceDivisor
    } else {
        rules.lateDifferenceDivisor
    }
}

/**
 * The signed advantage of x over y, scaled by the season divisor. Duels add one
 * to this to form a multiplier, so a zero difference leaves the base weights
 * untouched.
 */
@SpecRef("3.5")
fun strengthDifference(x: Double, y: Double, divisor: Double): Double = (x - y) / divisor
