package org.openfoot.engine.match

import org.openfoot.model.Half
import org.openfoot.model.Rng
import org.openfoot.model.SpecRef
import org.openfoot.model.randRange

/**
 * How long one match runs.
 *
 * Minutes are numbered from zero across the whole match rather than restarting
 * at the interval, so a minute index identifies a tick uniquely. The simulation
 * derives each minute's generator stream from that index, which is why it has
 * to be unique.
 */
@SpecRef("3.1")
data class MatchClock(
    val firstHalfMinutes: Int,
    val secondHalfMinutes: Int,
) {
    val totalMinutes: Int get() = firstHalfMinutes + secondHalfMinutes

    fun halfOf(minute: Int): Half =
        if (minute < firstHalfMinutes) Half.FIRST else Half.SECOND
}

/**
 * Draws the stoppage time of both halves.
 *
 * Drawn once at kick off rather than at the end of each half, in the order
 * section 3.1 lists them. The two halves get different spreads, so the second
 * half is on average two minutes longer than the first.
 */
@SpecRef("3.1")
fun matchClock(rng: Rng): MatchClock {
    val firstHalfStoppage = rng.randRange(FIRST_HALF_STOPPAGE_MIN, FIRST_HALF_STOPPAGE_MAX)
    val secondHalfStoppage = rng.randRange(SECOND_HALF_STOPPAGE_MIN, SECOND_HALF_STOPPAGE_MAX)
    return MatchClock(
        firstHalfMinutes = REGULATION_HALF_MINUTES + firstHalfStoppage,
        secondHalfMinutes = REGULATION_HALF_MINUTES + secondHalfStoppage,
    )
}

/**
 * Declared internal, not private: SeedStreamsTest derives the longest legal
 * match's minute count from this and the two stoppage maximums below, rather
 * than copying the 91 to 97 range as a second literal.
 */
@SpecRef("3.1")
internal const val REGULATION_HALF_MINUTES = 45

@SpecRef("3.1")
private const val FIRST_HALF_STOPPAGE_MIN = 0

@SpecRef("3.1")
internal const val FIRST_HALF_STOPPAGE_MAX = 2

@SpecRef("3.1")
private const val SECOND_HALF_STOPPAGE_MIN = 1

@SpecRef("3.1")
internal const val SECOND_HALF_STOPPAGE_MAX = 5
