package org.openfoot.model

/**
 * One row of a draw table: the draws that select this value.
 *
 * Section 3.8 writes every one of its tables as an if chain over a single
 * rand, and two of them lead with an equality test that a plain prefix table
 * cannot express. A list of explicit ranges, walked in order, expresses all of
 * them in one shape, and lets a test verify that a table covers every draw
 * exactly once instead of taking the boundaries on trust.
 *
 * Not every List<Band<T>> in the model is drawn against with rand. Section
 * 3.8's injury duration reuses this same shape for its age brackets, which is
 * read with pick() only and never with bound(); see bound()'s own docstring.
 */
@SpecRef("3.8")
data class Band<T>(val draws: IntRange, val value: T)

/**
 * The value the given draw selects. The first band whose range contains it
 * wins, so an overlapping table resolves the way the spec's if chain does.
 *
 * A draw outside every band throws rather than defaulting, because the only
 * way to get one is to have drawn against the wrong bound.
 */
@SpecRef("3.8")
fun <T> List<Band<T>>.pick(draw: Int): T = first { draw in it.draws }.value

/**
 * The bound to draw against: one past the last band's last draw.
 *
 * Only meaningful for a table that is actually drawn against with rand. A
 * table whose last band is a sentinel reaching to Int.MAX_VALUE, such as
 * section 3.8's injury age brackets, is never drawn against at all and must
 * never call this: adding one to Int.MAX_VALUE overflows. Read a table like
 * that with pick() only.
 */
@SpecRef("3.8")
fun <T> List<Band<T>>.bound(): Int = last().draws.last + 1
