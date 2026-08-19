package org.openfoot.engine.match

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Finding 6. simulateMatch derives one stream per minute by tag, plus three
 * fixed streams: SETUP_STREAM, PLAY_STREAM and the reserved DISCIPLINE_STREAM.
 * Nothing in the type system stops a future edit from picking a fixed stream
 * tag that collides with a minute index or with one of the other fixed
 * streams, which would make two draws that are supposed to be independent
 * come from the same fork. These tests lock the reservation down so such a
 * collision fails loudly here instead of silently making two minutes, or a
 * minute and the discipline draws once section 3.8 lands, play identically.
 */
class SeedStreamsTest {

    @Test
    fun `the three fixed streams are pairwise distinct`() {
        val streams = listOf(SETUP_STREAM, PLAY_STREAM, DISCIPLINE_STREAM)
        assertEquals(streams.size, streams.toSet().size, "the fixed streams must not collide: $streams")

        assertNotEquals(SETUP_STREAM, PLAY_STREAM)
        assertNotEquals(SETUP_STREAM, DISCIPLINE_STREAM)
        assertNotEquals(PLAY_STREAM, DISCIPLINE_STREAM)
    }

    /**
     * A minute index runs from zero up to, but not including, the longest
     * legal match's total minutes. SETUP_STREAM sharing a fork level with
     * those tags is only safe while no minute index can ever reach it, so this
     * derives the ceiling from the same constants matchClock draws from rather
     * than repeating the 91 to 97 range as a second literal.
     */
    @Test
    fun `SETUP_STREAM sits outside the range a minute index can reach`() {
        val longestMatchMinutes = REGULATION_HALF_MINUTES + FIRST_HALF_STOPPAGE_MAX +
            REGULATION_HALF_MINUTES + SECOND_HALF_STOPPAGE_MAX
        val highestPossibleMinuteIndex = longestMatchMinutes - 1

        assertTrue(
            SETUP_STREAM > highestPossibleMinuteIndex.toLong(),
            "SETUP_STREAM ($SETUP_STREAM) must be outside the reachable minute range " +
                "(0..$highestPossibleMinuteIndex), or a long match could fork the same stream twice",
        )
    }
}
