package org.openfoot.engine.lineup

import org.openfoot.engine.world.Player
import org.openfoot.engine.world.playerStyle
import org.openfoot.model.Position
import org.openfoot.model.Side
import org.openfoot.model.Trait

/**
 * Squads for the lineup tests.
 *
 * Everything section 5.4 does not read is held constant, so a squad in a test
 * reads as the list of positions and strengths that the sorting and the
 * filling actually depend on. Characteristics are chosen so that the derived
 * style of section 4.3 is the one named, because style decides which cells a
 * player is eligible for and picking traits at random would make eligibility
 * accidental.
 *
 * The styles these factories produce, all derived rather than declared:
 * keeper and centre back defensive, fullback offensive because of pace and
 * crossing, midfielder offensive because of passing and playmaking, forward
 * offensive because finishing and heading are neither the defensive pair nor
 * the winger triple. A defensive midfielder needs the tackling factory below,
 * which is why it exists at all.
 */
object Squads {

    fun of(vararg players: Player): List<Player> = players.toList()

    fun keeper(strength: Int) = player(Position.GOALKEEPER, strength, Trait.REFLEXES, Trait.POSITIONING)

    fun centreback(strength: Int) = player(Position.CENTREBACK, strength, Trait.MARKING, Trait.TACKLING)

    fun fullback(strength: Int) = player(Position.FULLBACK, strength, Trait.PACE, Trait.CROSSING)

    fun midfielder(strength: Int) = player(Position.MIDFIELDER, strength, Trait.PASSING, Trait.PLAYMAKING)

    fun holdingMidfielder(strength: Int) =
        player(Position.MIDFIELDER, strength, Trait.TACKLING, Trait.MARKING)

    fun forward(strength: Int) = player(Position.FORWARD, strength, Trait.FINISHING, Trait.HEADING)

    private fun player(
        position: Position,
        strength: Int,
        firstTrait: Trait,
        secondTrait: Trait,
    ) = Player(
        name = "Teste",
        age = 25,
        country = 3,
        position = position,
        side = Side.RIGHT,
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
