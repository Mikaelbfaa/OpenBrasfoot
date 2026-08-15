package org.openbrasfoot.model

/**
 * Points at the section of spec/SIMULATION-SPEC.md that a constant or formula
 * comes from, for example "3.6c" or "4.9".
 *
 * Every magic number in the simulation carries one. Review then reduces to a
 * single mechanical question: where in the spec is this number.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
)
annotation class SpecRef(val section: String)
