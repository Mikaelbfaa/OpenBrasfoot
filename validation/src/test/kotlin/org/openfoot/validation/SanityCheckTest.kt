package org.openfoot.validation

import org.openfoot.engine.match.MatchReport
import org.openfoot.engine.match.MatchSetup
import org.openfoot.engine.match.simulateMatch
import org.openfoot.model.SpecRef
import org.openfoot.model.SplitMix64Rng
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Section 3.16 says what a faithful reimplementation must produce over many
 * matches between two equivalent sides. Every test below is a claim about the
 * assembled engine rather than about one formula, which is what makes this the
 * first real check that the parts fit together.
 *
 * Deterministic, so it cannot be flaky: the same seeds always play the same
 * twenty thousand matches. A failure here is a real regression, never noise.
 *
 * Every band was set around a measured value and then checked against what
 * section 3.16 predicts. Three of them do not agree with it, and each one is
 * recorded rather than papered over:
 *
 * The tick count is 94, not the 92 of section 3.16, because section 3.1's own
 * stoppage draws average 94. See open question 28.
 *
 * The displayed possession share is 53.2 per cent, not 55. See open question
 * 29.
 *
 * The shot volumes and the goal averages fall short of section 3.16's figures
 * whenever the lineup does not fill every line to its fixed divisor, which a
 * four four two does not. See open question 30, and the last test in this file,
 * which reproduces section 3.16's figures exactly once the lines are full.
 */
class SanityCheckTest {

    @Test
    fun `a match runs about ninety four minutes`() {
        val ticks = MATCHES.mean { it.clock.totalMinutes }
        assertTrue(ticks in TICKS, "mean tick count was $ticks")
    }

    @Test
    fun `the home side takes about fifteen and a third shots`() {
        val shots = MATCHES.mean { it.stats.home.shots }
        assertTrue(shots in HOME_SHOTS, "home shots averaged $shots")
    }

    @Test
    fun `the away side takes about eleven and nine tenths shots`() {
        val shots = MATCHES.mean { it.stats.away.shots }
        assertTrue(shots in AWAY_SHOTS, "away shots averaged $shots")
    }

    @Test
    fun `the home side takes more shots than the away side`() {
        val home = MATCHES.mean { it.stats.home.shots }
        val away = MATCHES.mean { it.stats.away.shots }
        assertTrue(home > away, "home took $home shots against an away side on $away")
    }

    @Test
    fun `both sides score about a third over one goal`() {
        val home = MATCHES.mean { it.homeGoals }
        val away = MATCHES.mean { it.awayGoals }
        assertTrue(home in GOALS, "home goals averaged $home")
        assertTrue(away in GOALS, "away goals averaged $away")
    }

    @Test
    fun `the home shot volume advantage is cancelled by the worse conversion`() {
        val home = MATCHES.mean { it.homeGoals }
        val away = MATCHES.mean { it.awayGoals }
        assertTrue(
            abs(home - away) < GOAL_GAP_LIMIT,
            "the inverted home advantage should nearly cancel, got $home against $away",
        )
    }

    @Test
    fun `the home side converts worse than the away side`() {
        val homeRate = MATCHES.mean { it.homeGoals } / MATCHES.mean { it.stats.home.shots }
        val awayRate = MATCHES.mean { it.awayGoals } / MATCHES.mean { it.stats.away.shots }
        assertTrue(
            homeRate < awayRate,
            "the classic rules invert home advantage, so $homeRate must be under $awayRate",
        )
        assertTrue(homeRate in HOME_CONVERSION, "home conversion was $homeRate")
        assertTrue(awayRate in AWAY_CONVERSION, "away conversion was $awayRate")
    }

    @Test
    fun `the displayed possession favours the home side`() {
        val share = MATCHES.sumOf { it.stats.homePossessionShare() } / MATCHES.size
        assertTrue(share in POSSESSION_SHARE, "home possession share was $share")
    }

    @Test
    fun `every match is internally consistent`() {
        MATCHES.forEachIndexed { index, result ->
            val events = with(result.stats) {
                home.shots + away.shots + home.tackles + away.tackles +
                    home.misplacedPasses + away.misplacedPasses
            }
            assertEquals(result.clock.totalMinutes, events, "match $index")
        }
    }

    /**
     * Replays against EQUAL_SIDES_SETUP, the same MatchSetup instance MATCHES
     * was built from, rather than a fresh EqualSides.setup(). MatchPlayer is
     * deliberately reference equal, not value equal (see MatchSide.kt), so
     * two independently built lineups would make every match in again
     * compare unequal to its counterpart in MATCHES by shooter reference
     * alone, whatever the replay actually produced. Sharing the setup means
     * the only way this assertion fails is a genuine divergence.
     */
    @Test
    fun `the whole sample replays identically`() {
        val again = play(EQUAL_SIDES_SETUP)
        assertEquals(MATCHES.size, again.size)
        again.forEachIndexed { index, result ->
            assertEquals(MATCHES[index], result, "match $index")
        }
    }

    /**
     * The evidence behind open question 30.
     *
     * Section 3.16's shot volumes need the chance duel to compare an attack and
     * a defence of equal rating, which only happens when the defence has five
     * players over its divisor of five and the attack three over its divisor of
     * three. Put eleven players out that way and the figures of section 3.16
     * come back, rescaled from 46 possessions to the 47 that section 3.1
     * actually produces.
     *
     * That is what pins the shortfall in the four four two on section 3.4's
     * fixed divisors rather than on the assembly of the engine.
     */
    @Test
    fun `the figures of section 3 16 appear once every line sits on its divisor`() {
        val home = LINES_AT_DIVISOR.mean { it.stats.home.shots }
        val away = LINES_AT_DIVISOR.mean { it.stats.away.shots }
        val homeGoals = LINES_AT_DIVISOR.mean { it.homeGoals }
        val awayGoals = LINES_AT_DIVISOR.mean { it.awayGoals }
        assertTrue(home in FULL_LINE_HOME_SHOTS, "home shots averaged $home")
        assertTrue(away in FULL_LINE_AWAY_SHOTS, "away shots averaged $away")
        assertTrue(homeGoals in FULL_LINE_GOALS, "home goals averaged $homeGoals")
        assertTrue(awayGoals in FULL_LINE_GOALS, "away goals averaged $awayGoals")
    }

    private companion object {

        /**
         * Twenty thousand matches. Large enough that the standard error of the
         * shot means is around two hundredths of a shot, which is an order of
         * magnitude under the narrowest band here, and cheap enough that the
         * whole class runs in a couple of seconds.
         */
        @SpecRef("3.16")
        const val SAMPLE = 20000L

        /**
         * Measured 93.99205. Section 3.1 predicts exactly 94: forty five
         * minutes a half plus a mean stoppage of one and of three. Section
         * 3.16 says 92, which this band deliberately excludes. See open
         * question 28.
         */
        @SpecRef("3.1")
        val TICKS = 93.9..94.1

        /**
         * Measured 15.30865, against 15.30 derived from the measured tick
         * count: 46.996 possessions times 0.613733 for the possession duel
         * times 0.530435 for the chance duel. Section 3.16 says 16. See open
         * question 30.
         */
        @SpecRef("3.16")
        val HOME_SHOTS = 15.1..15.5

        /**
         * Measured 11.9297, against 11.89 derived the same way: 46.996 times
         * 0.55 times 0.46. Section 3.16 says 12.6. See open question 30.
         */
        @SpecRef("3.16")
        val AWAY_SHOTS = 11.75..12.10

        /**
         * Measured 1.3333 at home and 1.32355 away. Both are the shot volume
         * above times the conversion below. Section 3.16 says 1.4 for each,
         * and the difference is entirely the shot shortfall, since the
         * conversion rates do match. See open question 30.
         */
        @SpecRef("3.16")
        val GOALS = 1.28..1.38

        /**
         * Measured 0.00975. Section 3.6c predicts a near cancellation rather
         * than an exact one, so this is bounded rather than asserted equal, at
         * roughly four standard errors of the difference.
         */
        @SpecRef("3.6c")
        const val GOAL_GAP_LIMIT = 0.05

        /**
         * Measured 0.0870946. The exact value with no goals yet scored is
         * 5.5 over 62.605, which is 0.087852, and the anti blowout ladder
         * pulls the average a little under it. Section 3.15 says 8.8 per cent,
         * so this one agrees.
         */
        @SpecRef("3.15")
        val HOME_CONVERSION = 0.085..0.089

        /**
         * Measured 0.1109458, against an exact 5.5 over 49.495, which is
         * 0.111122. Section 3.15 says 11.1 per cent, so this one agrees too,
         * and the pair of them is the inverted home advantage of the classic
         * rules.
         */
        @SpecRef("3.15")
        val AWAY_CONVERSION = 0.109..0.113

        /**
         * Measured 0.5321090. Section 3.5's duel counter gives 53.2 per cent
         * for the home side. Section 3.16 says 55. The band excludes both 55
         * and the 50 that a share of ticks would give. See open question 29.
         */
        @SpecRef("3.5")
        val POSSESSION_SHARE = 0.525..0.540

        /**
         * Measured 16.3187, against 46.996 times 0.613733 times 0.565217,
         * which is 16.30. Section 3.16's 16, computed from 46 possessions,
         * would be 15.96.
         */
        @SpecRef("3.16")
        val FULL_LINE_HOME_SHOTS = 16.1..16.5

        /**
         * Measured 12.958, against 46.996 times 0.55 times 0.5, which is
         * 12.92. Section 3.16's 12.6 is the same arithmetic over 46
         * possessions rather than over the 47 section 3.1 produces.
         */
        @SpecRef("3.16")
        val FULL_LINE_AWAY_SHOTS = 12.80..13.15

        /** Measured 1.4212 at home and 1.43515 away. Section 3.16 says 1.4. */
        @SpecRef("3.16")
        val FULL_LINE_GOALS = 1.37..1.48

        /**
         * The one MatchSetup instance MATCHES is played against, held so that
         * the whole sample replays identically test can replay the same
         * fixture rather than an independently built one. MatchPlayer
         * compares by reference, not by value, so two separately built
         * lineups would make every replayed match compare unequal to the
         * original by shooter reference alone, regardless of what the replay
         * actually produced.
         */
        @SpecRef("3.16")
        val EQUAL_SIDES_SETUP: MatchSetup by lazy { EqualSides.setup() }

        /**
         * The sample: twenty thousand matches played with the four four two
         * lineup, which every test above except the last checks against
         * section 3.16's figures.
         */
        @SpecRef("3.16")
        val MATCHES: List<MatchReport> by lazy { play(EQUAL_SIDES_SETUP) }

        /**
         * The same sample size, played with the lineup whose defensive and
         * attacking lines exactly fill section 3.4's fixed divisors: five
         * defenders and three forwards.
         */
        @SpecRef("3.4")
        val LINES_AT_DIVISOR: List<MatchReport> by lazy { play(EqualSides.linesAtDivisorSetup()) }

        /**
         * One sample. The setup is immutable and carries no state, so building
         * it once and playing every seed against it gives the same matches as
         * rebuilding it per seed.
         */
        fun play(setup: MatchSetup): List<MatchReport> =
            (1L..SAMPLE).map { simulateMatch(setup, SplitMix64Rng(it)) }

        fun List<MatchReport>.mean(of: (MatchReport) -> Int): Double =
            sumOf { of(it).toDouble() } / size
    }
}
