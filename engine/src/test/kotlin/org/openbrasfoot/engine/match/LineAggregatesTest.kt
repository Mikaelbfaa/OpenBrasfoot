package org.openbrasfoot.engine.match

import org.openbrasfoot.model.Marking
import org.openbrasfoot.model.Position
import org.openbrasfoot.model.RuleSet
import org.openbrasfoot.model.RuleSets
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Golden vectors for spec section 3.4. Individual abilities are off throughout,
 * so a player of strength S rates exactly S divided by ten and every expected
 * value below is arithmetic anyone can redo on paper.
 */
class LineAggregatesTest {

    private val classic: RuleSet = RuleSets.CLASSIC
    private val modern: RuleSet = RuleSets.MODERN

    @Test
    fun `five defenders average out`() {
        val side = Lineups.sideOfSlots(listOf(2, 3, 4, 5, 6), strength = 50)
        assertEquals(5.0, defenceAggregate(side, classic), 1e-9)
    }

    @Test
    fun `four defenders still divide by five and lose a fifth`() {
        val side = Lineups.sideOfSlots(listOf(2, 3, 4, 5), strength = 50)
        assertEquals(4.0, defenceAggregate(side, classic), 1e-9)
    }

    @Test
    fun `a sixth defender is wasted entirely`() {
        val five = Lineups.sideOfSlots(listOf(2, 3, 4, 5, 6), strength = 50)
        val six = Lineups.sideOfSlots(listOf(2, 3, 4, 5, 6, 7), strength = 50)
        assertEquals(defenceAggregate(five, classic), defenceAggregate(six, classic), 1e-9)
    }

    @Test
    fun `fewer than three defenders collapses to the degenerate rating`() {
        val side = Lineups.sideOfSlots(listOf(2, 3), strength = 90)
        assertEquals(0.01, defenceAggregate(side, classic), 1e-9)
    }

    @Test
    fun `players are taken in lineup order not by strength`() {
        val weakFirst = Lineups.side(
            listOf(11, 12, 13).map { Lineups.player(it, strength = 30) } +
                listOf(14, 15, 16).map { Lineups.player(it, strength = 100) },
        )
        assertEquals(5.8, midfieldAggregate(weakFirst, classic), 1e-9)

        val strongFirst = Lineups.side(
            listOf(14, 15, 16).map { Lineups.player(it, strength = 100) } +
                listOf(11, 12, 13).map { Lineups.player(it, strength = 30) },
        )
        assertEquals(7.2, midfieldAggregate(strongFirst, classic), 1e-9)
    }

    @Test
    fun `the marking dial nudges the midfield total before the divisor`() {
        val slots = listOf(11, 12, 13, 14, 15)
        assertEquals(
            5.0,
            midfieldAggregate(Lineups.sideOfSlots(slots, 50, Marking.LIGHT), classic),
            1e-9,
        )
        assertEquals(
            5.008,
            midfieldAggregate(Lineups.sideOfSlots(slots, 50, Marking.HEAVY), classic),
            1e-9,
        )
        assertEquals(
            5.016,
            midfieldAggregate(Lineups.sideOfSlots(slots, 50, Marking.VERY_HEAVY), classic),
            1e-9,
        )
    }

    @Test
    fun `a shorthanded midfield ignores the marking bonus`() {
        val side = Lineups.sideOfSlots(listOf(11, 12), 50, Marking.VERY_HEAVY)
        assertEquals(0.01, midfieldAggregate(side, classic), 1e-9)
    }

    @Test
    fun `three forwards average out`() {
        val side = Lineups.sideOfSlots(listOf(19, 20, 21), strength = 60)
        assertEquals(6.0, attackAggregate(side, classic), 1e-9)
    }

    @Test
    fun `a lone striker is divided by three anyway`() {
        val side = Lineups.sideOfSlots(listOf(20), strength = 60)
        assertEquals(2.0, attackAggregate(side, classic), 1e-9)
    }

    @Test
    fun `an empty attack rates zero`() {
        val side = Lineups.sideOfSlots(listOf(1, 2, 3, 4, 5), strength = 60)
        assertEquals(0.0, attackAggregate(side, classic), 1e-9)
    }

    @Test
    fun `classic drops slot eighteen from the attack line`() {
        val side = Lineups.sideOfSlots(listOf(18, 25, 23), strength = 60)
        assertEquals(4.0, attackAggregate(side, classic), 1e-9)
    }

    @Test
    fun `modern counts slot eighteen so a three four three attacks with three`() {
        val side = Lineups.sideOfSlots(listOf(18, 25, 23), strength = 60)
        assertEquals(6.0, attackAggregate(side, modern), 1e-9)
    }

    @Test
    fun `a player in slot eighteen feeds no line under classic`() {
        val without = Lineups.sideOfSlots(listOf(1, 2, 3, 4, 11, 12, 13, 19), strength = 60)
        val with = Lineups.sideOfSlots(listOf(1, 2, 3, 4, 11, 12, 13, 19, 18), strength = 60)
        assertEquals(lineAggregates(without, classic), lineAggregates(with, classic))
    }

    @Test
    fun `a real keeper rates normally`() {
        val side = Lineups.sideOfSlots(listOf(1), strength = 70)
        assertEquals(7.0, keeperAggregate(side, classic), 1e-9)
    }

    @Test
    fun `an outfielder in goal collapses to a whole number`() {
        val side = Lineups.side(
            listOf(Lineups.player(slot = 1, strength = 70, position = Position.FORWARD)),
        )
        assertEquals(1.0, keeperAggregate(side, classic), 1e-9)
    }

    @Test
    fun `a weak outfielder in goal legitimately rates zero`() {
        val side = Lineups.side(
            listOf(Lineups.player(slot = 1, strength = 40, position = Position.FORWARD)),
        )
        assertEquals(0.0, keeperAggregate(side, classic), 1e-9)
    }

    @Test
    fun `even a world class outfielder in goal rates one`() {
        val side = Lineups.side(
            listOf(Lineups.player(slot = 1, strength = 100, position = Position.FORWARD)),
        )
        assertEquals(1.0, keeperAggregate(side, classic), 1e-9)
    }

    @Test
    fun `a missing keeper rates the degenerate value`() {
        val side = Lineups.sideOfSlots(listOf(2, 3, 4), strength = 70)
        assertEquals(0.1, keeperAggregate(side, classic), 1e-9)
    }

    @Test
    fun `bench entries are ignored by every line`() {
        val pitch = Lineups.FORMATION_4_4_2.map { Lineups.player(it, strength = 50) }
        val bench = listOf(26, 27, 28, 30, 36).map { Lineups.player(it, strength = 100) }
        assertEquals(
            lineAggregates(Lineups.side(pitch), classic),
            lineAggregates(Lineups.side(pitch + bench), classic),
        )
    }

    @Test
    fun `a full four four two produces all four ratings`() {
        val side = Lineups.sideOfSlots(Lineups.FORMATION_4_4_2, strength = 60)
        val aggregates = lineAggregates(side, classic)
        assertEquals(6.0, aggregates.keeper, 1e-9)
        assertEquals(4.8, aggregates.defence, 1e-9)
        assertEquals(4.8, aggregates.midfield, 1e-9)
        assertEquals(4.0, aggregates.attack, 1e-9)
    }
}
