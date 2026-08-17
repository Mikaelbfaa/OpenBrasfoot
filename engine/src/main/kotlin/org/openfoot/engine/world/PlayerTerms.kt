package org.openfoot.engine.world

import org.openfoot.model.Rng
import org.openfoot.model.SpecRef
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
