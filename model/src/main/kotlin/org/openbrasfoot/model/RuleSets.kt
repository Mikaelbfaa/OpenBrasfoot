package org.openbrasfoot.model

/**
 * Which set of rules a career runs under.
 *
 * This is the only file allowed to name a preset. Engine code takes a RuleSet
 * as a parameter and never asks which one it is, and an architecture test
 * enforces that.
 */
enum class RuleSetId {
    CLASSIC,
    MODERN,
}

/**
 * Home advantage as the original applies it to a shot.
 *
 * Two defects of the original live here together. The sign is inverted, so the
 * home side ends up with higher weights on both non goal outcomes and converts
 * worse than the visitor. And the wide weight is overwritten from the save
 * weight, which throws away the defence versus attack comparison in every match
 * that is not on neutral ground.
 *
 * Both are reproduced deliberately. See spec section 3.6c.
 */
@SpecRef("3.6c")
object ClassicShotHomeRule : ShotHomeRule {
    override fun adjust(
        multipliers: ShotMultipliers,
        advantage: HomeAdvantage,
        delta: Double,
    ): ShotMultipliers = when (advantage) {
        HomeAdvantage.NONE -> multipliers
        HomeAdvantage.POSSESSOR_HOME -> {
            val saved = multipliers.saved + delta
            ShotMultipliers(saved, saved + delta)
        }
        HomeAdvantage.POSSESSOR_AWAY -> {
            val saved = multipliers.saved - delta
            ShotMultipliers(saved, saved - delta)
        }
    }
}

/**
 * Home advantage applied the way the rest of the engine applies it: the side at
 * home gets lower weights on both non goal outcomes, so it converts better, and
 * the wide weight keeps the defence versus attack term it was computed from.
 */
@SpecRef("3.6c")
object ModernShotHomeRule : ShotHomeRule {
    override fun adjust(
        multipliers: ShotMultipliers,
        advantage: HomeAdvantage,
        delta: Double,
    ): ShotMultipliers = when (advantage) {
        HomeAdvantage.NONE -> multipliers
        HomeAdvantage.POSSESSOR_HOME ->
            ShotMultipliers(multipliers.saved - delta, multipliers.wide - delta)
        HomeAdvantage.POSSESSOR_AWAY ->
            ShotMultipliers(multipliers.saved + delta, multipliers.wide + delta)
    }
}

/**
 * The available rule sets.
 *
 * CLASSIC reproduces the original bug for bug and is the default. It is also
 * the only way to check the engine against the original, by comparing
 * statistical output, so it is not merely nostalgia.
 *
 * MODERN fixes what was clearly broken. It must stay a copy of CLASSIC with
 * only documented deltas, and a test asserts exactly that.
 */
object RuleSets {

    @SpecRef("3.4")
    val CLASSIC = RuleSet(
        id = RuleSetId.CLASSIC,

        keeperSlot = 1,
        defenceSlots = 2..9,
        midfieldSlots = 10..17,
        attackSlots = 19..25,
        centrebackSlots = 3..8,

        defenceTake = 5,
        defenceDivisor = 5.0,
        defenceMinimum = 3,
        midfieldTake = 5,
        midfieldDivisor = 5.0,
        midfieldMinimum = 3,
        attackTake = 3,
        attackDivisor = 3.0,

        shorthandedLineRating = 0.01,
        emptyAttackRating = 0.0,
        missingKeeperRating = 0.1,
        keeperOutOfPositionFactor = 0.2,
        markingMidfieldBonus = listOf(0.0, 0.04, 0.08),

        differenceDivisor = 8.0,
        lateDifferenceDivisor = 11.0,
        lateShotDifferenceDivisor = 10.0,
        compressionFirstSeason = 5,

        possessionBaseWeights = DuelBaseWeights(55.0, 45.0),
        homeDuelBonus = 0.3,
        duelWeightFloor = 0.2,

        chanceBaseWeights = DuelBaseWeights(50.0, 50.0),
        emptyLineDuelWeight = 0.1,
        chanceNoCentrebackWeight = 0.10,
        chanceOneCentrebackWeight = 0.05,

        shotBaseWeights = ShotBaseWeights(5.5, 35.55, 15.0),
        antiBlowoutLadder = listOf(
            AntiBlowoutStep(3, ShotBaseWeights(4.5, 40.55, 15.0)),
            AntiBlowoutStep(5, ShotBaseWeights(3.0, 40.55, 15.0)),
            AntiBlowoutStep(6, ShotBaseWeights(0.5, 40.55, 15.0)),
        ),
        outclassedGoalsAtLeast = 2,
        outclassedReputationGap = 2,
        outclassedWeights = ShotBaseWeights(3.0, 40.55, 15.0),
        shotHomeDelta = 0.1,
        shotHomeRule = ClassicShotHomeRule,
        savedNoCentrebackFactor = 0.2,
        savedOneCentrebackFactor = 0.4,
        missingShooterRating = 0.1,

        shooterSlotWeights = listOf(
            0, 0,
            1, 1, 1, 1, 1, 1, 1, 1,
            8,
            4, 4, 4,
            8, 8, 8, 8,
            22, 22, 22, 22, 22, 22, 22, 22,
        ),
        shooterEligibleSlots = 2..25,
        shooterFinishingBonus = 4,
        shooterHeadingBonus = 2,
        shooterHeadingDefenderBonus = 2,
    )

    /**
     * Exactly two deltas, both defects that live in the aggregate and shot code.
     *
     * The fixed line divisors of five, five and three are deliberately not
     * changed here. That is a balance decision rather than a defect, and it
     * would need a lever of its own.
     */
    @SpecRef("3.15")
    val MODERN = CLASSIC.copy(
        id = RuleSetId.MODERN,
        attackSlots = 18..25,
        shotHomeRule = ModernShotHomeRule,
    )
}
