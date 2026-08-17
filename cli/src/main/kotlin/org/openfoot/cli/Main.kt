package org.openfoot.cli

import kotlinx.serialization.json.Json
import org.openfoot.dataset.WorldDataset
import org.openfoot.engine.world.World
import org.openfoot.engine.world.generateWorld
import org.openfoot.importer.InstallationImporter
import java.io.File
import kotlin.system.exitProcess

/**
 * Entry point for the headless tools. Subcommands are added as the engine grows.
 * See the roadmap in README.md.
 */
fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "worldgen" -> worldgen(args.drop(1))
        "import" -> importInstallation(args.drop(1))
        "help", "--help" -> println(USAGE)
        null -> {
            println(USAGE)
            exitProcess(1)
        }

        else -> {
            System.err.println("openfoot-cli: unknown subcommand '${args[0]}'")
            System.err.println(USAGE)
            exitProcess(1)
        }
    }
}

private val USAGE = """
    usage: openfoot-cli import   --install <path> --out <path>
           openfoot-cli worldgen --dataset <path> --seed <number>

      import   reads your own installation of the original game and writes a
               dataset. Nothing is copied but numbers, and the files stay put.
      worldgen builds a world from a dataset and prints what came out. The same
               dataset and the same seed always print the same thing.
""".trimIndent()

/**
 * Reads an installation and writes a dataset.
 *
 * Everything the installation could not supply is printed rather than left for
 * the reader to discover, because a dataset with placeholder country levels
 * generates a world that looks right and is not.
 */
private fun importInstallation(args: List<String>) {
    val options = parseOptions(args)
    val install = options["--install"] ?: fail("import needs --install <path>")
    val out = options["--out"] ?: fail("import needs --out <path>")

    val root = File(install)
    if (!root.isDirectory) {
        fail("no installation directory at $install")
    }

    val result = try {
        InstallationImporter.importFrom(root)
    } catch (failure: Exception) {
        fail("could not import $install: ${failure.message}")
    }

    val json = Json { prettyPrint = true }
    File(out).writeText(json.encodeToString(result.dataset))

    println("clubs     ${result.dataset.clubs.size}")
    println("players   ${result.dataset.clubs.sumOf { it.squad.size }}")
    println("countries ${result.dataset.countries.size}")
    println("written   $out")
    if (result.notes.isNotEmpty()) {
        println("notes     ${result.notes.size}")
        result.notes.forEach { println("  $it") }
    }
}

/**
 * Reads a dataset, generates a world and describes it.
 *
 * Everything that can go wrong here is the user handing over a path or a
 * number, so each failure says which one and stops, rather than generating a
 * world from a default nobody asked for.
 */
private fun worldgen(args: List<String>) {
    val options = parseOptions(args)
    val path = options["--dataset"] ?: fail("worldgen needs --dataset <path>")
    val seedText = options["--seed"] ?: fail("worldgen needs --seed <number>")
    val seed = seedText.toLongOrNull() ?: fail("seed '$seedText' is not a number")

    val file = File(path)
    if (!file.isFile) {
        fail("no dataset file at $path")
    }

    val dataset = try {
        Json.decodeFromString<WorldDataset>(file.readText())
    } catch (failure: Exception) {
        fail("dataset at $path is not usable: ${failure.message}")
    }

    print(summarise(generateWorld(dataset, seed)))
}

private fun parseOptions(args: List<String>): Map<String, String> {
    val options = LinkedHashMap<String, String>()
    var index = 0
    while (index < args.size) {
        val flag = args[index]
        if (!flag.startsWith("--")) {
            fail("unexpected argument '$flag'")
        }
        options[flag] = args.getOrNull(index + 1) ?: fail("$flag needs a value")
        index += 2
    }
    return options
}

private fun fail(message: String): Nothing {
    System.err.println("openfoot-cli: $message")
    exitProcess(1)
}

/**
 * Describes a generated world in a form that is the same on every run.
 *
 * Nothing here reads a clock, a locale or a hash order, because the whole point
 * of printing it is that two runs can be compared with a diff. Clubs are listed
 * by reference rather than in dataset order for the same reason.
 */
internal fun summarise(world: World): String {
    val strengths = world.clubs.flatMap { club -> club.squad.map { it.strength } }.sorted()
    val builder = StringBuilder()

    builder.appendLine("seed      ${world.seed}")
    builder.appendLine("clubs     ${world.clubs.size}")
    builder.appendLine("players   ${world.playerCount}")

    if (strengths.isNotEmpty()) {
        builder.appendLine(
            "strength  min ${strengths.first()}  median ${strengths[strengths.size / 2]}  " +
                "max ${strengths.last()}",
        )
    }

    for (club in world.clubs.sortedBy { it.entry.ref }) {
        val best = club.squad.maxByOrNull { it.strength }
        builder.appendLine(
            "  ${club.entry.ref}  level ${club.entry.level}  players ${club.squad.size}  " +
                "best ${best?.strength ?: 0} ${best?.name.orEmpty()}",
        )
    }

    return builder.toString()
}
