/**
 * Keeps documents readable in a plain terminal, a diff and a code review.
 *
 * Portuguese letters and accents are welcome. What is rejected is the class of
 * typographic and mathematical symbols that render inconsistently and that
 * nobody can type: arrows, dashes that are not hyphens, comparison operators,
 * curly quotes and decorative marks.
 *
 * Every rejection names the plain replacement, so the fix is mechanical.
 */

plugins {
    base
}

val forbiddenInDocuments = mapOf(
    '→' to "->",
    '←' to "<-",
    '↑' to "up",
    '↓' to "desc",
    '↔' to "<->",
    '⇒' to "=>",
    '⇔' to "se e somente se",
    '≤' to "<=",
    '≥' to ">=",
    '≠' to "!=",
    '≈' to "~",
    '×' to "x",
    '÷' to "/",
    '±' to "+/-",
    '−' to "-",
    '—' to "-",
    '–' to "-",
    '‒' to "-",
    '‑' to "-",
    '‐' to "-",
    '·' to ",",
    '•' to "-",
    '…' to "...",
    '“' to "\"",
    '”' to "\"",
    '‘' to "'",
    '’' to "'",
    '«' to "\"",
    '»' to "\"",
    ' ' to "a normal space",
    '§' to "secao",
    '∑' to "soma",
    '∏' to "produto",
    '∈' to "em",
    '√' to "raiz",
    '∞' to "infinito",
    '²' to "^2",
    '³' to "^3",
    '✓' to "ok",
    '✔' to "ok",
    '✗' to "x",
    '✘' to "x",
    '⚠' to "Atencao:",
    '★' to "*",
    '☆' to "*",
    '™' to "(tm)",
    '©' to "(c)",
    '®' to "(r)",
    'Δ' to "dif",
    'Σ' to "soma",
)

val checkDocumentStyle = tasks.register("checkDocumentStyle") {
    group = "verification"
    description = "Fails on typographic symbols in markdown that have a plain ASCII equivalent."

    val documents = fileTree(projectDir) {
        include("**/*.md")
        exclude("**/build/**", "**/.git/**", "**/.gradle/**")
    }
    inputs.files(documents).withPathSensitivity(PathSensitivity.RELATIVE)

    doLast {
        val problems = mutableListOf<String>()

        documents.forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                line.forEachIndexed { column, character ->
                    val replacement = forbiddenInDocuments[character] ?: return@forEachIndexed
                    problems += buildString {
                        append(file.relativeTo(projectDir))
                        append(":")
                        append(index + 1)
                        append(":")
                        append(column + 1)
                        append(" uses '")
                        append(character)
                        append("', write ")
                        append(replacement)
                        append(" instead")
                    }
                }
            }
        }

        if (problems.isNotEmpty()) {
            throw GradleException(
                "Document style violations. Accented letters are fine, these symbols are not:\n" +
                    problems.take(40).joinToString("\n") { "  $it" } +
                    if (problems.size > 40) "\n  and ${problems.size - 40} more" else "",
            )
        }
    }
}

tasks.named("check") {
    dependsOn(checkDocumentStyle)
}
