package org.openfoot.model

/**
 * How a player interprets his position, which the original calls his style.
 *
 * Derived once from position and characteristics and then fixed for life. It
 * decides which cells a player is eligible for, and it selects which row of the
 * attribute table of section 4.2 generates him, so two fullbacks with the same
 * strength can come out with quite different attributes.
 *
 * The derivation itself is section 4.3's and lives with world generation in
 * the engine. Only the value type lives here, next to Position and Side,
 * because Slot.requiredStyle is one half of the section 3.2 grid table and the
 * grid is a model type: a slot has to be able to name the sub role it asks
 * for without the engine being on the classpath.
 */
@SpecRef("4.3")
enum class PlayerStyle {
    DEFENSIVE,
    OFFENSIVE,
    WINGER,
}
