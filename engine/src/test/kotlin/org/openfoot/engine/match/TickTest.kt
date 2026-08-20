package org.openfoot.engine.match

import org.openfoot.model.PlayerId
import org.openfoot.model.RuleSets
import org.openfoot.model.TeamSide
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The tick is scripted rather than sampled, so each path is pinned by the draws
 * it is entitled to make.
 *
 * The two sides are built at equal strength from Lineups, whose default
 * competition kind is a friendly, which is not neutral ground under section
 * 3.11. Home advantage therefore applies on the home side's ticks, and every
 * boundary below is worked out by hand from section 3.6 with that in mind.
 */
class TickTest {

    private fun evenSetup(season: Int = 1): MatchSetup {
        val home = Lineups.sideOfSlots(
            Lineups.FORMATION_4_4_2,
            strength = 50,
            context = Lineups.context(isHome = true),
        )
        val away = Lineups.sideOfSlots(
            Lineups.FORMATION_4_4_2,
            strength = 50,
            context = Lineups.context(isHome = false),
        )
        return MatchSetup(home, away, season = season, rules = RuleSets.CLASSIC)
    }

    @Test
    fun `a lost possession duel costs two draws and yields a loose ball`() {
        val rng = ScriptedRng(0.99, 0.10)
        val outcome = playTick(evenSetup(), TeamSide.HOME, goalsScoredByPossessor = 0, rng = rng)

        assertEquals(TeamSide.AWAY, outcome.possessionWinner)
        assertEquals(TeamSide.HOME, outcome.possessor)
        assertTrue(!outcome.isShot)
        assertEquals(2, rng.draws)
    }

    @Test
    fun `a tackle is credited to the side that was not in possession`() {
        val rng = ScriptedRng(0.99, 0.10)
        val outcome = playTick(evenSetup(), TeamSide.HOME, 0, rng)
        assertEquals(TickEvent.TACKLE, outcome.event)
    }

    @Test
    fun `the other half of the coin is a misplaced pass`() {
        val rng = ScriptedRng(0.99, 0.90)
        val outcome = playTick(evenSetup(), TeamSide.HOME, 0, rng)
        assertEquals(TickEvent.MISPLACED_PASS, outcome.event)
    }

    /**
     * Finding 3. The two tests above only pin that 0.10 lands on TACKLE and
     * 0.90 lands on MISPLACED_PASS, which is also true of a lopsided split, so
     * they cannot tell a 70/30 coin from the even one section 3.5 documents.
     * The coin is EVEN_COIN, weights [1.0, 1.0] with default multipliers of
     * [1.0, 1.0], so weightedPick's own contract fixes the boundary exactly:
     * total is 2.0, and index 0 (TACKLE) is picked while running (1.0) is
     * strictly greater than the draw (nextDouble times total), so the boundary
     * sits at a raw nextDouble of 0.5 and the comparison is strict. That is the
     * same weights and multipliers WeightedChoiceTest already exercises at
     * 0.49, 0.5 and 0.51, reused here through the real coin rather than the
     * primitive directly.
     */
    @Test
    fun `the loose ball coin is even right at one half, not some wider split`() {
        val justBelow = playTick(evenSetup(), TeamSide.HOME, 0, ScriptedRng(0.99, 0.49)).event
        val onTheBoundary = playTick(evenSetup(), TeamSide.HOME, 0, ScriptedRng(0.99, 0.5)).event
        val justAbove = playTick(evenSetup(), TeamSide.HOME, 0, ScriptedRng(0.99, 0.51)).event

        assertEquals(TickEvent.TACKLE, justBelow, "a draw just under 0.5 must still select TACKLE")
        assertEquals(
            TickEvent.MISPLACED_PASS,
            onTheBoundary,
            "weightedPick's strict comparison sends a draw exactly on the boundary to the " +
                "following outcome, MISPLACED_PASS",
        )
        assertEquals(TickEvent.MISPLACED_PASS, justAbove, "a draw just over 0.5 must select MISPLACED_PASS")
    }

    @Test
    fun `a won duel with no chance costs three draws`() {
        val rng = ScriptedRng(0.01, 0.99, 0.10)
        val outcome = playTick(evenSetup(), TeamSide.HOME, 0, rng)

        assertEquals(TeamSide.HOME, outcome.possessionWinner)
        assertTrue(!outcome.isShot)
        assertEquals(3, rng.draws)
    }

    @Test
    fun `a chance becomes a shot and costs four draws`() {
        val rng = ScriptedRng(0.01, 0.01, 0.50, 0.01)
        val outcome = playTick(evenSetup(), TeamSide.HOME, 0, rng)

        assertTrue(outcome.isShot)
        assertEquals(4, rng.draws)
    }

    @Test
    fun `every shot outcome maps onto a tick event`() {
        val events = listOf(0.01, 0.50, 0.99).map { shotDraw ->
            playTick(evenSetup(), TeamSide.HOME, 0, ScriptedRng(0.01, 0.01, 0.50, shotDraw)).event
        }
        assertEquals(3, events.toSet().size, "each shot outcome must map to its own event: $events")
        assertTrue(events.all { it != TickEvent.TACKLE && it != TickEvent.MISPLACED_PASS })
    }

    @Test
    fun `the possessor's own goals feed the anti blowout ladder`() {
        val fresh = playTick(evenSetup(), TeamSide.HOME, 0, ScriptedRng(0.01, 0.01, 0.50, 0.05))
        val leading = playTick(evenSetup(), TeamSide.HOME, 6, ScriptedRng(0.01, 0.01, 0.50, 0.05))

        assertEquals(TickEvent.GOAL, fresh.event)
        assertTrue(leading.event != TickEvent.GOAL, "six goals in must collapse the conversion rate")
    }

    @Test
    fun `the tick never alternates possession itself`() {
        val outcome = playTick(evenSetup(), TeamSide.AWAY, 0, ScriptedRng(0.99, 0.10))
        assertEquals(TeamSide.AWAY, outcome.possessor)
    }

    /**
     * A tick that ends in a shot names the player who took it, so that the
     * event log can credit a goal and section 3.14 can count a player's shots.
     * A tick that ends in a tackle or a misplaced pass names nobody, because
     * nobody shot.
     *
     * All four draws are derived by hand from RuleSets.CLASSIC and evenSetup's
     * fixture, which builds both sides on FORMATION_4_4_2 at strength 50 with
     * individual abilities off, so every player's own rating is 50 / 10 = 5.0.
     * A line's aggregate is not that rating, though: LineAggregates divides
     * each line's total by a fixed divisor rather than by the number of
     * players actually found there, and FORMATION_4_4_2 is short in every
     * line, so the aggregates below are worked out per line rather than
     * assumed equal to the player rating.
     *
     * FORMATION_4_4_2 is [1, 22, 24, 11, 13, 14, 16, 2, 9, 3, 5]. Walking it
     * against CLASSIC's slot ranges: the midfield line (10..17) picks up
     * slots 11, 13, 14, 16, four players, giving (4 * 5.0 + markingBonus) /
     * midfieldDivisor(5.0); the fixture's marking is the side() default,
     * Marking.LIGHT, whose bonus is markingMidfieldBonus[0] = 0.0, so midfield
     * = 20.0 / 5.0 = 4.0. The attack line (19..25) picks up slots 22, 24, two
     * players, giving (2 * 5.0) / attackDivisor(3.0) = 10.0 / 3.0 = 3.3333...
     * The defence line (2..9) picks up slots 2, 9, 3, 5, four players, giving
     * (4 * 5.0) / defenceDivisor(5.0) = 20.0 / 5.0 = 4.0. Both sides run the
     * identical formation, strength and marking, so these three numbers are
     * the same on both sides of the ball.
     *
     * Draw 1, the possession duel, HOME is possessor and isHome, and the
     * fixture is a friendly, which is not neutral ground, so home advantage
     * applies. Midfield is compared against midfield, and both sides' midfield
     * is the same 4.0 worked out above, so whatever that common value is, the
     * difference is 0 and it cancels: keep = 1.0 + (4.0 - 4.0) / 8.0 +
     * homeDuelBonus(0.3) = 1.3, win = 1.0 + 0 = 1.0. Base weights are
     * possessionBaseWeights(55.0, 45.0), so the total is 55 * 1.3 + 45 * 1.0 =
     * 116.5 and the POSSESSOR boundary sits at 71.5 / 116.5 = 0.61373..., the
     * same fraction DuelsTest pins for a home possessor at parity. A raw draw
     * of 0.01 (draw = 1.165) is far under that boundary, so HOME keeps the
     * ball.
     *
     * Draw 2, the chance duel, same possessor and advantage, compares the
     * possessor's attack (3.3333...) against the defender's defence (4.0),
     * and unlike the midfield duel these are not equal. attack = 1.0 +
     * (3.3333... - 4.0) / 8.0 + homeDuelBonus(0.3) = 1.0 - 0.08333... + 0.3 =
     * 1.21666..., defend = 1.0 + (4.0 - 3.3333...) / 8.0 = 1.0 + 0.08333... =
     * 1.08333.... Base weights are chanceBaseWeights(50.0, 50.0). The two
     * multipliers still sum to a fixed 2.3 regardless of the actual attack and
     * defence values, because the (a - d) / 8 and (d - a) / 8 terms are
     * negatives of each other and cancel on summation, leaving
     * 1.0 + 1.0 + homeDuelBonus(0.3) = 2.3 every time; that is why the total,
     * 50 * 2.3 = 115, comes out the same as a naive equal-lines guess would
     * give, and it is not a coincidence. The SHOT boundary, though, is not
     * fixed the same way, because it depends on attack alone rather than on
     * the sum: 50 * 1.21666... / 115 = 73 / 138 = 0.52899..., not the
     * 0.56521... a naive equal-lines assumption would produce. A raw draw of
     * 0.01 (draw = 1.15) is still far under 60.8333..., the SHOT weight
     * itself, so the chance becomes a shot regardless.
     *
     * Draw 3, shooter selection, draws only from CLASSIC.shooterSlotWeights
     * with no multiplier (selectShooter's weightedPick call supplies none), and
     * every player in this fixture carries Lineups.NEUTRAL_TRAITS, which the
     * fixture's own docstring says earns no shooter bonus, so the cell weight
     * is the whole weight. FORMATION_4_4_2 is
     * [1, 22, 24, 11, 13, 14, 16, 2, 9, 3, 5]; slot 1 is the keeper and falls
     * outside shooterEligibleSlots(2..25), so selectShooter's candidate list,
     * in lineup order, is slots [22, 24, 11, 13, 14, 16, 2, 9, 3, 5] with cell
     * weights [22, 22, 4, 4, 8, 8, 1, 1, 1, 1] read off shooterSlotWeights
     * (index 22 and 24 fall in the 22 block, 11 and 13 in the 4 block, 14 and
     * 16 in the 8 block, and 2, 9, 3, 5 are all in the 1 block). ShooterSelectionTest
     * already proves this fixture's total is 72. The first candidate, slot 22,
     * owns the running range [0, 22), which as a raw fraction of 72 is
     * [0, 0.30555...). A raw draw of 0.10 (draw = 7.2) sits well inside that
     * range and nowhere near its 22 boundary, so slot 22, the player built by
     * Lineups.player(22, 50) with id PlayerId(22), is the shooter.
     *
     * Draw 4, shot resolution, is not pinned to a particular outcome: isShot is
     * true for GOAL, SAVE and WIDE alike, and the shooter is already fixed by
     * draw 3, so any valid raw draw proves the point. 0.01 is used only to keep
     * the fixture uniform with the rest of this file's shot draws.
     */
    @Test
    fun `a tick that produces a shot names the shooter`() {
        val outcome = playTick(evenSetup(), TeamSide.HOME, 0, ScriptedRng(0.01, 0.01, 0.10, 0.01))

        assertTrue(outcome.isShot, "the scripted draws should have produced a shot")
        val shooter = assertNotNull(outcome.shooter, "a shot must name its shooter")
        assertEquals(22, shooter.slot.value, "the third draw should have picked the slot 22 forward")
        assertEquals(PlayerId(22), shooter.id)
    }

    /**
     * Reuses the lost possession duel script this file already derives at the
     * top: a raw draw of 0.99 is far above the 0.61373... POSSESSOR boundary
     * worked out there, so the duel is lost before a shooter is ever drawn.
     */
    @Test
    fun `a tick that produces no shot names nobody`() {
        val outcome = playTick(evenSetup(), TeamSide.HOME, 0, ScriptedRng(0.99, 0.10))

        assertTrue(!outcome.isShot, "the scripted draw should have lost the possession duel")
        assertNull(outcome.shooter, "a tick with no shot must name nobody")
    }
}
