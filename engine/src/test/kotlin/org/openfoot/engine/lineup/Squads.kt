package org.openfoot.engine.lineup

import org.openfoot.engine.world.Player
import org.openfoot.engine.world.playerStyle
import org.openfoot.model.Position
import org.openfoot.model.Side
import org.openfoot.model.Trait

/**
 * Squads for the lineup tests.
 *
 * Everything section 5.4's inner loop does not read is held constant: name,
 * age, country, contract, salary and market value never enter the search, so a
 * squad in a test reads as the list of positions, sides and strengths that the
 * sorting and the filling actually depend on. Characteristics are chosen so
 * that the derived style of section 4.3 is the one named, because style decides
 * which cells a player is eligible for and picking traits at random would make
 * eligibility accidental.
 *
 * Side is a parameter of every factory and not a constant, because fits() in
 * AutoLineup.kt reads it: a cell of a flank pair demands one side on the exact
 * pass and gives up on it on the next. It defaults to the right because most
 * tests here are about something else and want one fewer moving part, but a
 * default is not the same as a constant, and a fixture that made every player
 * right footed would leave the whole of Slot.requiredSide unpinned. The tests
 * that do discriminate that table pass the side explicitly on both flanks.
 *
 * The styles these factories produce, all derived rather than declared:
 * keeper and centre back defensive, fullback offensive because of pace and
 * crossing, midfielder offensive because of passing and playmaking, forward
 * offensive because finishing and heading are neither the defensive pair nor
 * the winger triple. The two tackling and marking factories exist because the
 * holding cells and the centre back cells ask for the defensive reading of a
 * midfielder and of a fullback, which none of the factories above produces.
 * Tackling and marking is not the only pair that would: by section 4.3 a
 * midfielder comes out defensive for any pair that carries tackling or marking
 * and none of passing, finishing, dribbling or playmaking, and a fullback for
 * those pairs and for pairs that carry nothing from either list, through the
 * closing branch. This pair is used because it is the plainest of them.
 *
 * The winger factory is the third reading of a forward. By section 4.3 a
 * forward carrying dribbling, pace or crossing and neither tackling nor
 * marking comes out a winger, which is the sub role cells 18 and 25 demand and
 * which no other position can ever hold.
 */
object Squads {

    fun of(vararg players: Player): List<Player> = players.toList()

    fun keeper(strength: Int, side: Side = Side.RIGHT) =
        player(Position.GOALKEEPER, strength, Trait.REFLEXES, Trait.POSITIONING, side)

    fun centreback(strength: Int, side: Side = Side.RIGHT) =
        player(Position.CENTREBACK, strength, Trait.MARKING, Trait.TACKLING, side)

    fun fullback(strength: Int, side: Side = Side.RIGHT) =
        player(Position.FULLBACK, strength, Trait.PACE, Trait.CROSSING, side)

    fun defensiveFullback(strength: Int, side: Side = Side.RIGHT) =
        player(Position.FULLBACK, strength, Trait.TACKLING, Trait.MARKING, side)

    fun midfielder(strength: Int, side: Side = Side.RIGHT) =
        player(Position.MIDFIELDER, strength, Trait.PASSING, Trait.PLAYMAKING, side)

    fun holdingMidfielder(strength: Int, side: Side = Side.RIGHT) =
        player(Position.MIDFIELDER, strength, Trait.TACKLING, Trait.MARKING, side)

    fun forward(strength: Int, side: Side = Side.RIGHT) =
        player(Position.FORWARD, strength, Trait.FINISHING, Trait.HEADING, side)

    fun winger(strength: Int, side: Side = Side.RIGHT) =
        player(Position.FORWARD, strength, Trait.DRIBBLING, Trait.PACE, side)

    private fun player(
        position: Position,
        strength: Int,
        firstTrait: Trait,
        secondTrait: Trait,
        side: Side,
    ) = Player(
        name = "Teste",
        age = 25,
        country = 3,
        position = position,
        side = side,
        firstTrait = firstTrait,
        secondTrait = secondTrait,
        starter = true,
        star = false,
        topWorld = false,
        talent = 5,
        style = playerStyle(position, firstTrait, secondTrait),
        strength = strength,
        abilities = List(7) { strength },
        contractDays = 700,
        salary = 1000L,
        marketValue = 100_000L,
    )
}
