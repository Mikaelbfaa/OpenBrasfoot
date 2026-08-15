package org.openbrasfoot.model

import kotlin.math.floor

/**
 * The single rounding function used by the whole simulation.
 *
 * The original rounds half up towards positive infinity, so -2.5 becomes -2 and
 * not -3. Kotlin offers several rounding helpers with quietly different
 * behaviour at the halfway point, and mixing them is exactly the kind of thing
 * that makes two builds disagree on match forty thousand. Every formula in the
 * engine calls this and nothing else.
 */
@SpecRef("0")
fun bfRound(value: Double): Int = floor(value + 0.5).toInt()

/**
 * Long returning variant for money, where values exceed the Int range.
 */
@SpecRef("0")
fun bfRoundLong(value: Double): Long = floor(value + 0.5).toLong()
