package org.openfoot.engine.world

import org.openfoot.model.Position
import org.openfoot.model.Rng
import org.openfoot.model.SpecRef
import org.openfoot.model.bfRound
import org.openfoot.model.bfRoundLong
import org.openfoot.model.rand

/**
 * How long a contract runs for a player who exists because the world was
 * created, rather than because he was signed, promoted or loaned.
 *
 * The spread matters more than the length. Every other event in section 4.7
 * hands out a round number of days, so if world creation did too, every squad
 * in the game would reach its renewal cliff on the same day of the first
 * season. The thirty day spread staggers them.
 */
@SpecRef("4.7")
fun initialContractDays(rng: Rng): Int = CONTRACT_BASE_DAYS + rng.rand(CONTRACT_SPREAD_DAYS)

@SpecRef("4.7")
private const val CONTRACT_BASE_DAYS = 210

@SpecRef("4.7")
private const val CONTRACT_SPREAD_DAYS = 30

/**
 * What a player is paid, in the unit the wage option selects.
 *
 * The formula produces a weekly figure and quadruples it when wages are shown
 * monthly, which is the default the original ships with. Nothing about the
 * player's own quality enters except his strength: age only ever subtracts, and
 * the position adjustment says a goalkeeper is cheaper than a midfielder of
 * identical strength at the same club.
 *
 * The youth multiplier of section 4.8 is absent for the same reason it is
 * absent from the strength formula: youth squads are not generated yet.
 */
@SpecRef("4.8")
fun salary(
    strength: Int,
    age: Int,
    position: Position,
    star: Boolean,
    topWorld: Boolean,
    clubLevel: Int,
    division: Int?,
    majorLeagueCountry: Boolean,
    monthlyWages: Boolean,
): Long {
    var base = divisionBase(division, majorLeagueCountry)
    if (clubLevel > RICH_CLUB_LEVEL) {
        base += RICH_CLUB_BONUS
    }
    base += positionAdjustment(position)
    base = bfRound(SALARY_BASE_HALVING * base)

    val core = strength.toLong() * SALARY_STRENGTH_FACTOR * base
    val starBonus = if (star || topWorld) strength.toLong() * STAR_SALARY_FACTOR else 0L

    var wage = if (age < DECLINE_AGE) {
        core + starBonus
    } else {
        core - (age - DECLINE_AGE).toLong() * AGE_PENALTY_PER_YEAR + starBonus
    }

    wage = maxOf(wage, SALARY_FLOOR)
    if (topWorld) {
        wage = bfRoundLong(wage * TOP_WORLD_SALARY_MULTIPLIER)
    }
    if (monthlyWages) {
        wage *= MONTHLY_MULTIPLIER
    }
    return wage
}

/**
 * The pay scale of the division, which is the only place the five countries
 * the spec singles out are treated differently from everyone else.
 */
@SpecRef("4.8")
private fun divisionBase(division: Int?, majorLeagueCountry: Boolean): Int =
    if (majorLeagueCountry) {
        when (division) {
            1 -> 750
            2 -> 550
            3 -> 500
            4, 5 -> 450
            else -> 350
        }
    } else {
        when (division) {
            1 -> 600
            2 -> 500
            3 -> 450
            4, 5 -> 400
            else -> 350
        }
    }

@SpecRef("4.8")
internal fun positionAdjustment(position: Position): Int = when (position) {
    Position.GOALKEEPER -> -70
    Position.FULLBACK -> -30
    Position.CENTREBACK -> -40
    Position.MIDFIELDER -> 0
    Position.FORWARD -> -50
}

/** A club above this level pays a premium. No data file expresses one. */
@SpecRef("4.8")
internal const val RICH_CLUB_LEVEL = 20

@SpecRef("4.8")
private const val RICH_CLUB_BONUS = 50

@SpecRef("4.8")
private const val SALARY_BASE_HALVING = 0.5

@SpecRef("4.8")
private const val SALARY_STRENGTH_FACTOR = 2

@SpecRef("4.8")
private const val STAR_SALARY_FACTOR = 250

@SpecRef("4.8")
private const val AGE_PENALTY_PER_YEAR = 300

@SpecRef("4.8")
private const val SALARY_FLOOR = 500L

@SpecRef("4.8")
private const val TOP_WORLD_SALARY_MULTIPLIER = 1.4

@SpecRef("4.8")
private const val MONTHLY_MULTIPLIER = 4

/** The age past which the salary formula starts subtracting. */
@SpecRef("4.8")
internal const val DECLINE_AGE = 32
