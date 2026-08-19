package org.openfoot.engine.match

import org.openfoot.model.Rng
import org.openfoot.model.RuleSets
import org.openfoot.model.SplitMix64Rng
import org.openfoot.model.TeamSide
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The loop is exercised with a real generator, because what is worth asserting
 * here is that it holds together over a whole match: the counters agree with
 * each other, the length obeys the clock, and the same seed gives the same
 * match.
 */
class MatchSimulationTest {

    private fun setup(homeStrength: Int = 50, awayStrength: Int = 50, season: Int = 1) =
        MatchSetup(
            home = Lineups.sideOfSlots(
                Lineups.FORMATION_4_4_2,
                homeStrength,
                context = Lineups.context(isHome = true),
            ),
            away = Lineups.sideOfSlots(
                Lineups.FORMATION_4_4_2,
                awayStrength,
                context = Lineups.context(isHome = false),
            ),
            season = season,
            rules = RuleSets.CLASSIC,
        )

    @Test
    fun `the same seed plays the same match`() {
        val first = simulateMatch(setup(), SplitMix64Rng(7))
        val second = simulateMatch(setup(), SplitMix64Rng(7))
        assertEquals(first, second)
    }

    @Test
    fun `a different seed plays a different match`() {
        val results = (1L..40L).map { simulateMatch(setup(), SplitMix64Rng(it)) }
        assertTrue(results.toSet().size > 1, "forty seeds produced one identical match")
    }

    /**
     * Finding 1. rng is a seed source that simulateMatch forks once and never
     * consumes further, so passing the SAME instance to two calls replays the
     * identical match both times: nothing about the first call's draws moves
     * what the second call sees. This is the footgun the docstring now warns
     * about, pinned so a future change that starts threading the parent's
     * state through the loop is caught here instead of by a silent repeated
     * fixture later.
     */
    @Test
    fun `passing the same Rng instance to two calls replays the identical match`() {
        val sharedRng = SplitMix64Rng(11)

        val first = simulateMatch(setup(), sharedRng)
        val second = simulateMatch(setup(), sharedRng)

        assertEquals(first, second, "the same Rng instance must not make the second call diverge")
    }

    /**
     * Finding 1's other half: the correct pattern. A caller simulating several
     * matches from one generator, such as a round of fixtures, must fork a
     * fresh child per match rather than pass the parent instance twice, and
     * doing so does give different matches.
     */
    @Test
    fun `forking a fresh child per match gives different matches`() {
        val seasonRng = SplitMix64Rng(11)

        val first = simulateMatch(setup(), seasonRng.fork(1L))
        val second = simulateMatch(setup(), seasonRng.fork(2L))

        assertTrue(first != second, "forking per match should not reproduce the same fixture twice")
    }

    @Test
    fun `the match runs exactly as many ticks as the clock says`() {
        repeat(200) { seed ->
            val result = simulateMatch(setup(), SplitMix64Rng(seed.toLong()))
            val ticks = result.stats.home.possessionsWon + result.stats.away.possessionsWon
            assertEquals(result.clock.totalMinutes, ticks, "seed $seed")
        }
    }

    @Test
    fun `every match lasts between ninety one and ninety seven minutes`() {
        repeat(500) { seed ->
            val minutes = simulateMatch(setup(), SplitMix64Rng(seed.toLong())).clock.totalMinutes
            assertTrue(minutes in 91..97, "seed $seed ran $minutes minutes")
        }
    }

    @Test
    fun `shots equal on target plus wide for both sides`() {
        repeat(200) { seed ->
            val stats = simulateMatch(setup(), SplitMix64Rng(seed.toLong())).stats
            listOf(stats.home, stats.away).forEach { side ->
                assertEquals(side.shots, side.onTarget + side.wide, "seed $seed")
            }
        }
    }

    @Test
    fun `goals never exceed shots on target`() {
        repeat(200) { seed ->
            val stats = simulateMatch(setup(), SplitMix64Rng(seed.toLong())).stats
            assertTrue(stats.home.goals <= stats.home.onTarget, "seed $seed")
            assertTrue(stats.away.goals <= stats.away.onTarget, "seed $seed")
        }
    }

    @Test
    fun `the scoreline agrees with the counters`() {
        repeat(200) { seed ->
            val result = simulateMatch(setup(), SplitMix64Rng(seed.toLong()))
            assertEquals(result.stats.home.goals, result.homeGoals, "seed $seed")
            assertEquals(result.stats.away.goals, result.awayGoals, "seed $seed")
        }
    }

    @Test
    fun `each side is possessor for half the ticks give or take one`() {
        repeat(200) { seed ->
            val result = simulateMatch(setup(), SplitMix64Rng(seed.toLong()))
            val total = result.clock.totalMinutes
            val homeTicks = (0 until total).count { minute ->
                possessorAt(result.startingPossessor, minute) == TeamSide.HOME
            }
            assertTrue(
                homeTicks * 2 in (total - 1)..(total + 1),
                "seed $seed gave the home side $homeTicks of $total ticks",
            )
        }
    }

    @Test
    fun `no side ever counts more than the whole match in tackles and passes`() {
        repeat(200) { seed ->
            val result = simulateMatch(setup(), SplitMix64Rng(seed.toLong()))
            val events = with(result.stats) {
                home.shots + away.shots +
                    home.tackles + away.tackles +
                    home.misplacedPasses + away.misplacedPasses
            }
            assertEquals(result.clock.totalMinutes, events, "seed $seed")
        }
    }

    @Test
    fun `fouls stay at zero over a whole match`() {
        val stats = simulateMatch(setup(), SplitMix64Rng(3)).stats
        assertEquals(0, stats.home.fouls)
        assertEquals(0, stats.away.fouls)
    }

    @Test
    fun `a much stronger side outshoots a much weaker one over many matches`() {
        val strong = (1L..300L).sumOf {
            simulateMatch(setup(homeStrength = 90, awayStrength = 30), SplitMix64Rng(it))
                .stats.home.shots
        }
        val weak = (1L..300L).sumOf {
            simulateMatch(setup(homeStrength = 90, awayStrength = 30), SplitMix64Rng(it))
                .stats.away.shots
        }
        assertTrue(strong > weak, "strong side took $strong shots against $weak")
    }

    /**
     * Finding 1's replacement for the tautological alternation test. That test
     * recomputed the expected possessor from startingPossessor and totalMinutes
     * alone, using the very formula the loop is supposed to follow, so it would
     * stay green even if the loop's alternation became conditional on the tick's
     * outcome. This test instead forces every tick in the whole match to score a
     * goal, and observes the resulting split.
     *
     * GOAL_WINNING_DRAW is derived below to sit under every boundary the shot,
     * chance and possession weightedPick calls can produce at two equal
     * strengths of fifty, so at this constant the possessor always wins the
     * possession duel, always works a chance, and always scores. Every tick is
     * therefore a goal for whoever is in possession that tick, and under
     * section 3.5's unconditional alternation the two sides take turns, so the
     * goals split between them differing by at most one. If alternation were
     * instead conditional on winning the duel, the possessor would never change
     * and one side would score every single minute while the other stayed at
     * zero, so this fails loudly under that regression.
     */
    @Test
    fun `every tick scores for whoever is in possession, and the goals split within one`() {
        val result = simulateMatch(setup(), FixedRng(GOAL_WINNING_DRAW))

        val homeGoals = result.stats.home.goals
        val awayGoals = result.stats.away.goals

        assertEquals(
            result.clock.totalMinutes,
            homeGoals + awayGoals,
            "expected every one of the ${result.clock.totalMinutes} ticks to score, " +
                "got $homeGoals home and $awayGoals away",
        )
        assertTrue(
            abs(homeGoals - awayGoals) <= 1,
            "unconditional alternation should split the goals within one of each " +
                "other, got $homeGoals home and $awayGoals away",
        )
        assertTrue(
            homeGoals > 0 && awayGoals > 0,
            "one side scored every tick and the other never did: $homeGoals-$awayGoals",
        )
    }

    /**
     * Finding 2a's decisive case, stated exactly as the review found it: a
     * TACKLE credited to the possessor instead of to the side that did not have
     * the ball would pass every other test in this file, because they all use
     * two equal strength sides where either crediting produces plausible
     * numbers. Calling record directly, with a hand built TickOutcome, is what
     * catches it. Both possessors are checked so a bug that is symmetric in
     * TeamSide, such as always crediting home, cannot hide behind one case.
     */
    @Test
    fun `record credits a tackle to the side that did not have the ball`() {
        val awayHadTheBall = MatchStats().record(TickOutcome(TeamSide.HOME, TeamSide.HOME, TickEvent.TACKLE))
        assertEquals(0, awayHadTheBall.home.tackles, "the possessor must not be credited with the tackle")
        assertEquals(1, awayHadTheBall.away.tackles, "the non possessor should be credited with the tackle")

        val homeHadTheBall = MatchStats().record(TickOutcome(TeamSide.AWAY, TeamSide.AWAY, TickEvent.TACKLE))
        assertEquals(1, homeHadTheBall.home.tackles, "the non possessor should be credited with the tackle")
        assertEquals(0, homeHadTheBall.away.tackles, "the possessor must not be credited with the tackle")
    }

    /**
     * Finding 2a in full: every one of the five TickEvent values, folded for
     * both possessors, checked against exactly the side the docstring on
     * record names. GOAL, SAVE, WIDE and MISPLACED_PASS all belong to the
     * possessor; TACKLE alone belongs to the side that did not have the ball.
     * Each case also checks that the other side's matching counter stayed at
     * zero, so a bug that credits both sides at once cannot hide either.
     */
    @Test
    fun `record credits every event to exactly the side its docstring names`() {
        for (possessor in listOf(TeamSide.HOME, TeamSide.AWAY)) {
            val defender = possessor.opponent

            val afterGoal = MatchStats().record(TickOutcome(possessor, possessor, TickEvent.GOAL))
            assertEquals(1, afterGoal.of(possessor).goals, "GOAL: possessor $possessor")
            assertEquals(1, afterGoal.of(possessor).shots, "GOAL: possessor $possessor")
            assertEquals(1, afterGoal.of(possessor).onTarget, "GOAL: possessor $possessor")
            assertEquals(0, afterGoal.of(defender).goals, "GOAL must not touch the defender: possessor $possessor")
            assertEquals(0, afterGoal.of(defender).shots, "GOAL must not touch the defender: possessor $possessor")

            val afterSave = MatchStats().record(TickOutcome(possessor, possessor, TickEvent.SAVE))
            assertEquals(1, afterSave.of(possessor).shots, "SAVE: possessor $possessor")
            assertEquals(1, afterSave.of(possessor).onTarget, "SAVE: possessor $possessor")
            assertEquals(0, afterSave.of(possessor).goals, "SAVE must not score: possessor $possessor")
            assertEquals(0, afterSave.of(defender).shots, "SAVE must not touch the defender: possessor $possessor")

            val afterWide = MatchStats().record(TickOutcome(possessor, possessor, TickEvent.WIDE))
            assertEquals(1, afterWide.of(possessor).shots, "WIDE: possessor $possessor")
            assertEquals(1, afterWide.of(possessor).wide, "WIDE: possessor $possessor")
            assertEquals(0, afterWide.of(defender).shots, "WIDE must not touch the defender: possessor $possessor")

            val afterTackle = MatchStats().record(TickOutcome(possessor, possessor, TickEvent.TACKLE))
            assertEquals(0, afterTackle.of(possessor).tackles, "TACKLE must not credit the possessor: $possessor")
            assertEquals(1, afterTackle.of(defender).tackles, "TACKLE: defender of $possessor")

            val afterMisplaced = MatchStats().record(TickOutcome(possessor, possessor, TickEvent.MISPLACED_PASS))
            assertEquals(1, afterMisplaced.of(possessor).misplacedPasses, "MISPLACED_PASS: possessor $possessor")
            assertEquals(
                0,
                afterMisplaced.of(defender).misplacedPasses,
                "MISPLACED_PASS must not touch the defender: possessor $possessor",
            )
        }
    }
}

private fun possessorAt(starting: TeamSide, minute: Int): TeamSide =
    if (minute % 2 == 0) starting else starting.opponent

/**
 * A draw so low it wins the first outcome of every weightedPick call the
 * engine makes at two equal strengths of fifty. Derived at the tightest point
 * the match can reach:
 *
 * The shot resolution's GOAL boundary at a fresh scoreline, possessor at home,
 * under CLASSIC, is base weight 5.5 against a total of 62.605 (5.5 * 1.0 +
 * 35.55 * 1.1 + 15.0 * 1.2, the 1.1 and 1.2 coming from the home shot rule's
 * plus one tenth on saved and plus two tenths on wide). That boundary only
 * gets tighter as the anti blowout ladder bites: once the possessing side has
 * scored six or more, CLASSIC's last ladder rung fixes the goal weight at 0.5
 * against a total of 63.105 (0.5 * 1.0 + 40.55 * 1.1 + 15.0 * 1.2), a boundary
 * of about 0.0079. No later rung exists, so that is the tightest the whole
 * match can ever produce, on either side, however many goals a side reaches
 * in a ninety one to ninety seven minute match. The possession duel's and the
 * chance duel's boundaries are far looser (about 0.61 and 0.57 for the side
 * at home, since both only add the home duel bonus on top of an even
 * strength comparison), so the shot ladder's floor governs.
 *
 * 0.001 sits safely under 0.0079, with roughly an eightfold margin, so the
 * possessor wins the possession duel, wins the chance duel and scores on
 * every tick, for both sides, for as many goals as either side can reach.
 */
private const val GOAL_WINNING_DRAW = 0.001

/**
 * Test double whose draws never move. nextDouble always returns the same
 * constant and fork always returns this same instance, so a whole match run
 * through it is fully determined by that one constant: every minuteRng, every
 * PLAY_STREAM fork and every duel draw inside a tick all resolve to it.
 * nextInt always returns zero, which keeps the once per match draws (the
 * starting possessor and both halves' stoppage) fixed too, so the match
 * length is deterministic as well.
 */
private class FixedRng(private val value: Double) : Rng {
    override fun nextBits(): Long = 0L

    override fun nextInt(bound: Int): Int = 0

    override fun nextDouble(): Double = value

    override fun fork(tag: Long): Rng = this
}
