package org.openbrasfoot.engine.match

import org.openbrasfoot.model.Rng
import org.openbrasfoot.model.SpecRef

/**
 * Picks one outcome from base weights scaled by multipliers.
 *
 * Every probabilistic decision in the match engine goes through here, so the
 * contract is fixed and must not change, or saved replays from older versions
 * stop reproducing:
 *
 * The draw is u equals nextDouble times the total. The comparison is strict, so
 * a u that lands exactly on a cumulative boundary selects the following index.
 * Exactly one draw is consumed per call, always, whatever the outcome. A total
 * that is not positive returns the last index rather than throwing, because the
 * engine must never fail in the middle of a match.
 */
@SpecRef("3.5")
fun weightedPick(baseWeights: DoubleArray, multipliers: DoubleArray, rng: Rng): Int {
    require(baseWeights.isNotEmpty()) { "weightedPick needs at least one outcome" }
    require(baseWeights.size == multipliers.size) {
        "expected ${baseWeights.size} multipliers, got ${multipliers.size}"
    }

    var total = 0.0
    for (index in baseWeights.indices) {
        total += baseWeights[index] * multipliers[index]
    }

    val draw = rng.nextDouble() * total
    if (total <= 0.0) {
        return baseWeights.size - 1
    }

    var running = 0.0
    for (index in baseWeights.indices) {
        running += baseWeights[index] * multipliers[index]
        if (running > draw) {
            return index
        }
    }
    return baseWeights.size - 1
}

/**
 * Convenience for the case where the weights are already final.
 */
@SpecRef("3.5")
fun weightedPick(weights: DoubleArray, rng: Rng): Int =
    weightedPick(weights, DoubleArray(weights.size) { 1.0 }, rng)
