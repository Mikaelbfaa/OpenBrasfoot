package org.openfoot.cli

/**
 * Entry point for the headless tools. Subcommands are added as the engine grows.
 * See the roadmap in README.md.
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("openfoot-cli: no subcommand given")
        return
    }
    println("openfoot-cli: unknown subcommand '${args[0]}'")
}
