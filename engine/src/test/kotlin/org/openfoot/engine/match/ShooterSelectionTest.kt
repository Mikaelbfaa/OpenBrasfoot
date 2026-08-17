package org.openfoot.engine.match

import org.openfoot.model.RuleSets
import org.openfoot.model.SplitMix64Rng
import org.openfoot.model.Trait
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShooterSelectionTest {

    private val rules = RuleSets.CLASSIC

    @Test
    fun `cell weights follow the spec table`() {
        assertEquals(1, shooterWeight(Lineups.player(2, 50), rules))
        assertEquals(1, shooterWeight(Lineups.player(9, 50), rules))
        assertEquals(8, shooterWeight(Lineups.player(10, 50), rules))
        assertEquals(4, shooterWeight(Lineups.player(11, 50), rules))
        assertEquals(8, shooterWeight(Lineups.player(14, 50), rules))
        assertEquals(8, shooterWeight(Lineups.player(17, 50), rules))
        assertEquals(22, shooterWeight(Lineups.player(18, 50), rules))
        assertEquals(22, shooterWeight(Lineups.player(25, 50), rules))
    }

    @Test
    fun `finishing adds four`() {
        val striker = Lineups.player(20, 50, firstTrait = Trait.FINISHING, secondTrait = Trait.PACE)
        assertEquals(26, shooterWeight(striker, rules))
    }

    @Test
    fun `finishing wins outright over heading`() {
        val both = Lineups.player(20, 50, firstTrait = Trait.FINISHING, secondTrait = Trait.HEADING)
        assertEquals(26, shooterWeight(both, rules))
    }

    @Test
    fun `a heading centre back gets the extra defender bonus`() {
        val stopper = Lineups.player(5, 50, firstTrait = Trait.HEADING, secondTrait = Trait.TACKLING)
        assertEquals(5, shooterWeight(stopper, rules))
    }

    @Test
    fun `a heading midfielder gets only the plain bonus`() {
        val midfielder = Lineups.player(14, 50, firstTrait = Trait.HEADING, secondTrait = Trait.PASSING)
        assertEquals(10, shooterWeight(midfielder, rules))
    }

    @Test
    fun `a fullback heading is not a defender for this bonus`() {
        val fullback = Lineups.player(2, 50, firstTrait = Trait.HEADING, secondTrait = Trait.PACE)
        assertEquals(3, shooterWeight(fullback, rules))
    }

    @Test
    fun `the trait counts in either position`() {
        val first = Lineups.player(20, 50, firstTrait = Trait.FINISHING, secondTrait = Trait.PACE)
        val second = Lineups.player(20, 50, firstTrait = Trait.PACE, secondTrait = Trait.FINISHING)
        assertEquals(shooterWeight(first, rules), shooterWeight(second, rules))
    }

    @Test
    fun `the keeper and the bench carry no cell weight`() {
        assertEquals(0, shooterWeight(Lineups.player(1, 50), rules))
        assertEquals(0, shooterWeight(Lineups.player(30, 50), rules))
    }

    @Test
    fun `the four four two weights total seventy two`() {
        val side = Lineups.sideOfSlots(Lineups.FORMATION_4_4_2, strength = 50)
        val total = side.lineup
            .filter { it.slot.value in rules.shooterEligibleSlots }
            .sumOf { shooterWeight(it, rules) }
        assertEquals(72, total)
    }

    @Test
    fun `the keeper is never drawn`() {
        val side = Lineups.sideOfSlots(Lineups.FORMATION_4_4_2, strength = 50)
        val rng = SplitMix64Rng(3)
        repeat(20_000) {
            val shooter = selectShooter(side, rules, rng)
            assertTrue(shooter != null && shooter.slot.value != 1, "the keeper was drawn")
        }
    }

    @Test
    fun `a striker takes about twenty two of every seventy two shots`() {
        val side = Lineups.sideOfSlots(Lineups.FORMATION_4_4_2, strength = 50)
        val rng = SplitMix64Rng(5)
        val draws = 100_000
        val striker = (1..draws).count { selectShooter(side, rules, rng)?.slot?.value == 22 }
        val share = striker.toDouble() / draws
        assertTrue(abs(share - 22.0 / 72.0) < 0.006, "expected about 0.3056, measured $share")
    }

    @Test
    fun `defenders share a small slice of the shots`() {
        val side = Lineups.sideOfSlots(Lineups.FORMATION_4_4_2, strength = 50)
        val rng = SplitMix64Rng(13)
        val draws = 100_000
        val defenders = (1..draws).count {
            selectShooter(side, rules, rng)?.slot?.value in listOf(2, 9, 3, 5)
        }
        val share = defenders.toDouble() / draws
        assertTrue(abs(share - 4.0 / 72.0) < 0.004, "expected about 0.0556, measured $share")
    }

    @Test
    fun `a bench striker with finishing is never drawn`() {
        val pitch = Lineups.FORMATION_4_4_2.map { Lineups.player(it, 50) }
        val benchStriker = Lineups.player(
            slot = 30,
            strength = 99,
            firstTrait = Trait.FINISHING,
            secondTrait = Trait.PACE,
        )
        val side = Lineups.side(pitch + benchStriker)
        val rng = SplitMix64Rng(17)
        repeat(20_000) {
            assertTrue(selectShooter(side, rules, rng)?.slot?.value != 30, "a bench player shot")
        }
    }

    @Test
    fun `slot eighteen can shoot even though it feeds no line`() {
        val side = Lineups.sideOfSlots(Lineups.FORMATION_3_4_3, strength = 50)
        val rng = SplitMix64Rng(19)
        val drawn = (1..5_000).any { selectShooter(side, rules, rng)?.slot?.value == 18 }
        assertTrue(drawn, "the slot eighteen forward never shot")
    }

    @Test
    fun `a side of only a keeper has nobody to shoot`() {
        val side = Lineups.sideOfSlots(listOf(1), strength = 50)
        assertNull(selectShooter(side, rules, SplitMix64Rng(1)))
    }
}
