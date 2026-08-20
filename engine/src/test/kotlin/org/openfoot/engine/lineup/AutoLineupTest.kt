package org.openfoot.engine.lineup

import org.openfoot.engine.match.MatchPlayer
import org.openfoot.engine.world.Player
import org.openfoot.model.Position
import org.openfoot.model.RuleSet
import org.openfoot.model.RuleSets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Section 5.4 sorts the pool once, by strength descending and energy
 * descending, and then walks the cells of the formation in order taking the
 * first player still available who fits. The consequence the spec calls out is
 * that because the formation lists name the forwards before the defenders, the
 * forwards pick from the top of the pool and the defenders get the leftovers.
 * That is asserted directly below, because a lineup builder that sorted by
 * need instead would look more sensible and would be wrong.
 *
 * The second thing asserted here is defect 7 of section 3.15. A cell searches
 * its own position first, relaxing side and then style, and only then walks
 * the position cascade. Classic never reaches the pass that ignores style, so
 * a holding midfield cell with no defensive midfielder left gives up on
 * midfielders entirely and cascades to a centre back. Modern reaches it and
 * fields the midfielder. Both are asserted, because that pair is the whole
 * reason the pass count is a rule set field.
 */
class AutoLineupTest {

    private val fourFourTwo = Formations.byId(4)

    /**
     * Formation 4 orders its cells 1, 22, 24, 11, 13, 14, 16, 2, 9, 3, 5:
     * the keeper, then two forward cells, then two holding midfield cells and
     * two attacking ones, then the fullbacks, then the centre backs.
     */
    private fun fill(
        squad: List<Player>,
        availability: Availability = Availability.FULL_SQUAD,
        rules: RuleSet = RuleSets.CLASSIC,
    ) = fillEleven(squad, fourFourTwo, rules, availability)

    private fun playerAt(slot: Int, eleven: List<MatchPlayer>) =
        eleven.first { it.slot.value == slot }

    /**
     * Pool order, worked out from the squad below rather than from the
     * implementation: 90, 80, 70 and 65 are midfielders, then the lone forward
     * on 60, then the last midfielder on 55, and then the five on 50 in squad
     * order, because nothing breaks a tie once energy is equal too.
     */
    private fun squadShortOfForwards() = Squads.of(
        Squads.keeper(strength = 50),
        Squads.forward(strength = 60),
        Squads.midfielder(strength = 90),
        Squads.midfielder(strength = 80),
        Squads.midfielder(strength = 70),
        Squads.midfielder(strength = 65),
        Squads.midfielder(strength = 55),
        Squads.fullback(strength = 50),
        Squads.fullback(strength = 50),
        Squads.centreback(strength = 50),
        Squads.centreback(strength = 50),
    )

    @Test
    fun `a squad short of forwards fills its forward cells with the best midfielders`() {
        val eleven = fill(squadShortOfForwards())

        assertEquals(60, playerAt(22, eleven).strength, "the one real forward takes the first cell")
        assertEquals(
            90,
            playerAt(24, eleven).strength,
            "the second forward cell cascades onto the strongest midfielder, because the " +
                "forward cells come before the midfield cells in the formation list",
        )
        assertEquals(
            Position.MIDFIELDER,
            playerAt(24, eleven).naturalPosition,
            "and he is a midfielder improvising, not a forward that was hiding",
        )
    }

    /**
     * The midfield cells of formation 4 are two holding cells, 11 and 13,
     * before the two attacking cells, 14 and 16. Every midfielder in this
     * squad is offensive, so no holding cell has a natural candidate.
     *
     * Under classic the search for cell 11 runs midfielder with side and
     * style, then midfielder with style, and then stops: the pass that would
     * ignore style is the one section 3.15 item 7 says the loop bound never
     * reaches. So it cascades. Fullback fails too, both fullbacks being
     * offensive; forward is empty by then, the lone forward having taken cell
     * 22 and the strongest midfielder cell 24; centre back matches on the
     * first pass, style defensive and no side demanded by a central cell. A
     * centre back on 50 therefore plays as a holding midfielder while an 80
     * sits.
     *
     * Under modern the third pass exists, so cell 11 takes the 80 and the
     * centre backs stay at the back.
     */
    @Test
    fun `classic gives up on the position before it gives up on the style`() {
        val classic = fill(squadShortOfForwards(), rules = RuleSets.CLASSIC)
        val modern = fill(squadShortOfForwards(), rules = RuleSets.MODERN)

        assertEquals(
            Position.CENTREBACK,
            playerAt(11, classic).naturalPosition,
            "classic cascades out of midfield rather than ignore the style it asked for",
        )
        assertEquals(50, playerAt(11, classic).strength, "and the centre backs in this squad are on 50")

        assertEquals(
            Position.MIDFIELDER,
            playerAt(11, modern).naturalPosition,
            "modern reaches the pass that ignores style and keeps a midfielder in midfield",
        )
        assertEquals(
            80,
            playerAt(11, modern).strength,
            "the strongest midfielder left once cell 24 has taken the 90",
        )
    }

    @Test
    fun `a natural fullback keeps the fullback cell whatever his style`() {
        val eleven = fill(squadShortOfForwards())

        assertEquals(
            Position.FULLBACK,
            playerAt(2, eleven).naturalPosition,
            "cells 2 and 9 name a side and no sub role, so an offensive fullback fits them",
        )
        assertEquals(Position.FULLBACK, playerAt(9, eleven).naturalPosition, "and the same on the left")
    }

    @Test
    fun `a holding midfielder takes the holding cell ahead of a stronger playmaker`() {
        val squad = Squads.of(
            Squads.keeper(strength = 50),
            Squads.forward(strength = 70),
            Squads.forward(strength = 70),
            Squads.midfielder(strength = 90),
            Squads.midfielder(strength = 85),
            Squads.holdingMidfielder(strength = 60),
            Squads.holdingMidfielder(strength = 55),
            Squads.fullback(strength = 50),
            Squads.fullback(strength = 50),
            Squads.centreback(strength = 50),
            Squads.centreback(strength = 50),
        )

        val eleven = fill(squad)

        assertEquals(60, playerAt(11, eleven).strength, "the stronger holding midfielder takes cell 11")
        assertEquals(55, playerAt(13, eleven).strength, "and the other takes cell 13")
        assertEquals(90, playerAt(14, eleven).strength, "the playmakers take the attacking cells")
        assertEquals(85, playerAt(16, eleven).strength, "in strength order")
    }

    @Test
    fun `a cell is filled by the strongest compatible player left`() {
        val squad = Squads.of(
            Squads.keeper(strength = 50),
            Squads.forward(strength = 70),
            Squads.forward(strength = 90),
            Squads.midfielder(strength = 60),
            Squads.midfielder(strength = 60),
            Squads.midfielder(strength = 60),
            Squads.midfielder(strength = 60),
            Squads.fullback(strength = 50),
            Squads.fullback(strength = 50),
            Squads.centreback(strength = 50),
            Squads.centreback(strength = 50),
        )

        val eleven = fill(squad)

        assertEquals(90, playerAt(22, eleven).strength, "the first forward cell takes the stronger")
        assertEquals(70, playerAt(24, eleven).strength, "the second takes what is left")
    }

    @Test
    fun `an unavailable player is never picked`() {
        val squad = Squads.of(
            Squads.keeper(strength = 50),
            Squads.forward(strength = 99),
            Squads.forward(strength = 70),
            Squads.forward(strength = 60),
            Squads.midfielder(strength = 60),
            Squads.midfielder(strength = 60),
            Squads.midfielder(strength = 60),
            Squads.midfielder(strength = 60),
            Squads.fullback(strength = 50),
            Squads.fullback(strength = 50),
            Squads.centreback(strength = 50),
            Squads.centreback(strength = 50),
        )
        val injured = Availability { index, _ -> PlayerAvailability(canPlay = index != 1, energy = 100) }

        val eleven = fill(squad, injured)

        assertTrue(
            eleven.none { it.strength == 99 },
            "the strongest player is unavailable and must not appear",
        )
        assertEquals(11, eleven.size, "eleven are still fielded")
    }

    @Test
    fun `energy breaks a tie in strength and nothing else does`() {
        val squad = Squads.of(
            Squads.keeper(strength = 50),
            Squads.forward(strength = 70),
            Squads.forward(strength = 70),
            Squads.midfielder(strength = 60),
            Squads.midfielder(strength = 60),
            Squads.midfielder(strength = 60),
            Squads.midfielder(strength = 60),
            Squads.fullback(strength = 50),
            Squads.fullback(strength = 50),
            Squads.centreback(strength = 50),
            Squads.centreback(strength = 50),
        )
        val tired = Availability { index, _ ->
            PlayerAvailability(canPlay = true, energy = if (index == 1) 40 else 100)
        }

        val eleven = fill(squad, tired)

        assertEquals(
            2,
            playerAt(22, eleven).id.value,
            "the two forwards are equally strong, so the fresher one takes the first cell",
        )
    }

    @Test
    fun `eleven are fielded even when nobody fits anything`() {
        val squad = List(11) { Squads.keeper(strength = 50) }

        val eleven = fill(Squads.of(*squad.toTypedArray()))

        assertEquals(11, eleven.size, "the original has no legality check and fields eleven regardless")
        assertEquals(
            11,
            eleven.map { it.slot.value }.distinct().size,
            "and never puts two of them in one cell",
        )
        assertEquals(
            11,
            eleven.map { it.id.value }.distinct().size,
            "and never fields one player twice",
        )
    }

    @Test
    fun `every player fielded keeps his index in the squad as his identity`() {
        val squad = Squads.of(
            Squads.keeper(strength = 50),
            Squads.forward(strength = 70),
            Squads.forward(strength = 60),
            Squads.midfielder(strength = 60),
            Squads.midfielder(strength = 60),
            Squads.midfielder(strength = 60),
            Squads.midfielder(strength = 60),
            Squads.fullback(strength = 50),
            Squads.fullback(strength = 50),
            Squads.centreback(strength = 50),
            Squads.centreback(strength = 50),
        )

        val eleven = fill(squad)

        for (player in eleven) {
            assertEquals(
                player.strength,
                squad[player.id.value].strength,
                "identity ${player.id} should index back to the player it was made from",
            )
        }
    }

    /**
     * The one chain section 5.4 writes out, keeper then centre back then
     * fullback, asserted at its first step. This squad has no keeper at all
     * and a defensive fullback available, so a chain that tried fullback
     * before centre back would seat him in goal instead.
     */
    @Test
    fun `the keeper cell falls to a centre back, which is the chain the spec writes`() {
        val squad = Squads.of(
            Squads.centreback(strength = 70),
            Squads.centreback(strength = 69),
            Squads.centreback(strength = 68),
            Squads.defensiveFullback(strength = 58),
            Squads.fullback(strength = 57),
            Squads.holdingMidfielder(strength = 73),
            Squads.holdingMidfielder(strength = 72),
            Squads.midfielder(strength = 75),
            Squads.midfielder(strength = 74),
            Squads.forward(strength = 80),
            Squads.forward(strength = 79),
        )

        val eleven = fill(squad)

        assertEquals(
            Position.CENTREBACK,
            playerAt(1, eleven).naturalPosition,
            "the keeper chain tries centre back before fullback",
        )
        assertEquals(70, playerAt(1, eleven).strength, "and takes the strongest of them")
    }

    /**
     * Cell 5 is the second centre back cell of formation 4 and this squad has
     * one centre back, so cell 5 cascades. The reserve keeper and a defensive
     * fullback are both left and both satisfy what the cell asks, defensive
     * and no side, so the answer is decided by nothing except the order of
     * the centre back chain. Fullback comes before keeper there, and the
     * keeper goes last in every outfield chain.
     */
    @Test
    fun `a centre back cell prefers a spare fullback to the reserve keeper`() {
        val squad = Squads.of(
            Squads.keeper(strength = 60),
            Squads.keeper(strength = 55),
            Squads.fullback(strength = 58),
            Squads.fullback(strength = 57),
            Squads.defensiveFullback(strength = 56),
            Squads.centreback(strength = 70),
            Squads.holdingMidfielder(strength = 73),
            Squads.holdingMidfielder(strength = 72),
            Squads.midfielder(strength = 75),
            Squads.midfielder(strength = 74),
            Squads.forward(strength = 80),
            Squads.forward(strength = 79),
        )

        val eleven = fill(squad)

        assertEquals(
            Position.FULLBACK,
            playerAt(5, eleven).naturalPosition,
            "the centre back chain tries fullback before it empties the bench of keepers",
        )
        assertEquals(56, playerAt(5, eleven).strength, "the one fullback the fullback cells left")
        assertTrue(
            eleven.none { it.naturalPosition == Position.GOALKEEPER && it.slot.value != 1 },
            "and no keeper is fielded outside the goal",
        )
    }

    /**
     * The mirror of the case above, one step further along a different chain.
     * Cell 9 is the left fullback cell and this squad has one fullback, taken
     * by cell 2. A spare centre back and a spare midfielder are both left and
     * the cell asks for no sub role, so again only the order of the chain
     * decides, and the fullback chain tries centre back before midfielder.
     */
    @Test
    fun `a fullback cell prefers a spare centre back to a spare midfielder`() {
        val squad = Squads.of(
            Squads.keeper(strength = 60),
            Squads.fullback(strength = 58),
            Squads.centreback(strength = 70),
            Squads.centreback(strength = 69),
            Squads.centreback(strength = 68),
            Squads.midfielder(strength = 75),
            Squads.midfielder(strength = 74),
            Squads.midfielder(strength = 71),
            Squads.holdingMidfielder(strength = 73),
            Squads.holdingMidfielder(strength = 72),
            Squads.forward(strength = 80),
            Squads.forward(strength = 79),
        )

        val eleven = fill(squad)

        assertEquals(
            Position.CENTREBACK,
            playerAt(9, eleven).naturalPosition,
            "the fullback chain tries centre back before midfielder",
        )
        assertEquals(70, playerAt(9, eleven).strength, "the strongest centre back, the pool being sorted")
    }

    /**
     * Cell 14 is an attacking midfield cell and this squad has no offensive
     * midfielder, so it cascades with a spare fullback and a spare forward
     * both available and both offensive. The midfield chain tries fullback
     * first, fullback being the nearer position on the line the game arranges
     * the five on.
     *
     * The same squad exercises the catch all at the other end. Cell 5 wants a
     * defensive centre back, and by the time it is reached the defence and the
     * midfield are empty and the only man left is an offensive forward, who
     * fits nothing the cell asked for and plays there anyway.
     */
    @Test
    fun `a midfield cell tries a fullback before a forward, and the last cell takes whoever is left`() {
        val squad = Squads.of(
            Squads.keeper(strength = 60),
            Squads.fullback(strength = 58),
            Squads.fullback(strength = 57),
            Squads.fullback(strength = 56),
            Squads.centreback(strength = 70),
            Squads.centreback(strength = 69),
            Squads.holdingMidfielder(strength = 73),
            Squads.holdingMidfielder(strength = 72),
            Squads.forward(strength = 80),
            Squads.forward(strength = 79),
            Squads.forward(strength = 78),
        )

        val eleven = fill(squad)

        assertEquals(
            Position.FULLBACK,
            playerAt(14, eleven).naturalPosition,
            "the midfield chain tries fullback before forward",
        )
        assertEquals(58, playerAt(14, eleven).strength, "the strongest fullback, none being used yet")
        assertEquals(
            Position.FORWARD,
            playerAt(5, eleven).naturalPosition,
            "the last cell finds nobody in any chain and takes the man left over",
        )
        assertEquals(78, playerAt(5, eleven).strength, "who is the third forward")
    }

    /**
     * The most expensive thing the classic pass count does, and the case I
     * argued could not happen before it was run. The squad is section 5.7 ideal
     * shape and nothing about it is contrived: two keepers, three offensive
     * fullbacks, three centre backs, five offensive midfielders, three
     * forwards.
     *
     * The two holding cells find no defensive midfielder and cascade onto two
     * of the three centre backs. The fullback cells take the fullbacks. Cell 3
     * takes the last centre back. Cell 5 is then a centre back cell with no
     * centre back left, and it refuses the spare fullback, the spare
     * midfielders and the spare forward on sub role, every one of them being
     * offensive, until the chain reaches the keeper and the reserve keeper is
     * defensive.
     *
     * This is the chain and not the catch all: the catch all would have taken
     * the strongest man left, the 76 midfielder, rather than a 48 keeper. Under
     * modern the third pass keeps the midfielders in midfield and no keeper
     * leaves the goal.
     */
    @Test
    fun `a defence cell falls to the reserve keeper when everyone left is offensive`() {
        val squad = Squads.of(
            Squads.keeper(strength = 62),
            Squads.keeper(strength = 48),
            Squads.fullback(strength = 66),
            Squads.fullback(strength = 64),
            Squads.fullback(strength = 63),
            Squads.centreback(strength = 68),
            Squads.centreback(strength = 67),
            Squads.centreback(strength = 65),
            Squads.midfielder(strength = 80),
            Squads.midfielder(strength = 78),
            Squads.midfielder(strength = 76),
            Squads.midfielder(strength = 75),
            Squads.midfielder(strength = 74),
            Squads.forward(strength = 82),
            Squads.forward(strength = 79),
            Squads.forward(strength = 77),
        )

        val classic = fill(squad, rules = RuleSets.CLASSIC)
        val modern = fill(squad, rules = RuleSets.MODERN)

        assertEquals(
            listOf(62, 82, 79, 68, 67, 80, 78, 66, 64, 65, 48),
            classic.map { it.strength },
            "cells 1, 22, 24, 11, 13, 14, 16, 2, 9, 3 and 5 in that order",
        )
        assertEquals(
            Position.GOALKEEPER,
            playerAt(5, classic).naturalPosition,
            "the second centre back cell reaches the end of its chain and takes the reserve keeper",
        )
        assertTrue(
            classic.none { it.strength == 76 },
            "and it is the chain, not the catch all, which would have taken the 76 midfielder",
        )
        assertTrue(
            modern.none { it.naturalPosition == Position.GOALKEEPER && it.slot.value != 1 },
            "modern fields no keeper outside the goal",
        )
    }

    /**
     * A sixteen man squad with a natural player for every cell any formation
     * asks for. This is the case where a defect in the cascade shows up as a
     * cell left empty or a player fielded twice, and it covers all twelve
     * formations rather than only the one the rest of this class uses.
     */
    @Test
    fun `every formation fields eleven distinct players from a whole squad`() {
        val squad = Squads.of(
            Squads.keeper(strength = 60),
            Squads.keeper(strength = 55),
            Squads.fullback(strength = 60),
            Squads.fullback(strength = 59),
            Squads.fullback(strength = 58),
            Squads.centreback(strength = 70),
            Squads.centreback(strength = 69),
            Squads.centreback(strength = 68),
            Squads.midfielder(strength = 75),
            Squads.midfielder(strength = 74),
            Squads.holdingMidfielder(strength = 73),
            Squads.holdingMidfielder(strength = 72),
            Squads.midfielder(strength = 71),
            Squads.forward(strength = 80),
            Squads.forward(strength = 79),
            Squads.forward(strength = 78),
        )

        for (formation in Formations.ALL) {
            val eleven = fillEleven(squad, formation, RuleSets.CLASSIC)
            assertEquals(11, eleven.size, "${formation.name} fields eleven")
            assertEquals(
                11,
                eleven.map { it.id.value }.distinct().size,
                "${formation.name} fields eleven different players",
            )
            assertEquals(
                formation.slots.map { it.value },
                eleven.map { it.slot.value },
                "${formation.name} fills its cells in list order",
            )
        }
    }
}
