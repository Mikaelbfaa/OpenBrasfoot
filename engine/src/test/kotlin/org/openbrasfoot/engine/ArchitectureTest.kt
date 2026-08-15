package org.openbrasfoot.engine

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards the properties that make the simulation portable, deterministic and
 * replayable. These run on every pull request and are cheaper than any amount
 * of documentation about how the engine is supposed to be written.
 *
 * A violation is not a style problem. Each of these leads to a career that
 * cannot be reproduced from its seed, which breaks bug reporting for everyone.
 */
class ArchitectureTest {

    private val sources: List<File> = File("src/main/kotlin")
        .walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .toList()

    @Test
    fun `engine performs no input or output and does not read the clock`() {
        val banned = listOf(
            "java.io",
            "java.nio.file",
            "java.time",
            "kotlinx.io",
            "androidx.compose",
            "org.slf4j",
            "System.currentTimeMillis",
            "System.nanoTime",
        )
        assertNoOccurrences(banned, "the engine must stay free of I/O, UI and wall clock access")
    }

    @Test
    fun `engine uses only the project rng`() {
        val banned = listOf(
            "java.util.Random",
            "kotlin.random.Random",
            "Math.random",
            "Random()",
        )
        assertNoOccurrences(
            banned,
            "all randomness must come from org.openbrasfoot.model.Rng so careers replay from a seed",
        )
    }

    @Test
    fun `engine avoids hash containers whose iteration order is unspecified`() {
        val banned = listOf(
            "hashMapOf",
            "hashSetOf",
            "HashMap(",
            "HashSet(",
            ": HashMap",
            ": HashSet",
        )
        assertNoOccurrences(
            banned,
            "iteration order would vary between runs, use LinkedHashMap, sorted collections or arrays",
        )
    }

    @Test
    fun `engine uses only the arithmetic the spec needs`() {
        val banned = listOf(
            "Math.pow",
            "Math.exp",
            "Math.sin",
            "Math.cos",
            "Math.log",
            "kotlin.math.pow",
            "kotlin.math.exp",
        )
        assertNoOccurrences(
            banned,
            "every formula in the spec is plus, minus, times, divide, round, min and max only",
        )
    }

    @Test
    fun `engine never branches on the ruleset identity`() {
        val offenders = sources
            .filter { it.name != "RuleSets.kt" }
            .mapNotNull { file ->
                val hits = file.readLines()
                    .withIndex()
                    .filter { (_, line) ->
                        val stripped = line.substringBefore("//")
                        stripped.contains("RuleSetId.") || stripped.contains("ruleSet.id ==")
                    }
                    .map { (index, _) -> "${file.name}:${index + 1}" }
                if (hits.isEmpty()) null else hits
            }
            .flatten()

        assertTrue(
            offenders.isEmpty(),
            "A divergence between rulesets belongs in a RuleSet field, not in an if. Found: $offenders",
        )
    }

    private fun assertNoOccurrences(banned: List<String>, why: String) {
        val offenders = mutableListOf<String>()
        sources.forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val stripped = line.substringBefore("//")
                banned.forEach { token ->
                    if (stripped.contains(token)) {
                        offenders += "${file.name}:${index + 1} uses $token"
                    }
                }
            }
        }
        assertTrue(offenders.isEmpty(), "$why. Found: $offenders")
    }
}
