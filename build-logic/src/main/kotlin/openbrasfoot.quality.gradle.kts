/**
 * Enforces the project comment style so review never has to argue about it.
 *
 * Comments are plain ASCII prose. No em dashes, no curly quotes, no arrows and
 * no markdown syntax. Markdown belongs in .md files, where it renders.
 */

val markdownInComments = listOf(
    "`" to "backtick",
    "**" to "markdown emphasis",
    "](" to "markdown link",
)

val checkCommentStyle = tasks.register("checkCommentStyle") {
    group = "verification"
    description = "Fails on non ASCII characters or markdown syntax inside code comments."

    val sources = fileTree(projectDir) {
        include("src/**/*.kt", "src/**/*.kts", "*.gradle.kts")
        exclude("**/build/**")
    }
    inputs.files(sources).withPathSensitivity(PathSensitivity.RELATIVE)

    doLast {
        val problems = mutableListOf<String>()

        sources.forEach { file ->
            var inBlock = false
            file.readLines().forEachIndexed { index, rawLine ->
                val comment = StringBuilder()
                var rest = rawLine

                if (inBlock) {
                    val end = rest.indexOf("*/")
                    if (end >= 0) {
                        comment.append(rest, 0, end)
                        rest = rest.substring(end + 2)
                        inBlock = false
                    } else {
                        comment.append(rest)
                        rest = ""
                    }
                }

                while (rest.isNotEmpty()) {
                    val lineComment = rest.indexOf("//")
                    val blockComment = rest.indexOf("/*")
                    if (lineComment >= 0 && (blockComment < 0 || lineComment < blockComment)) {
                        comment.append(rest.substring(lineComment + 2))
                        rest = ""
                    } else if (blockComment >= 0) {
                        var body = rest.substring(blockComment + 2)
                        if (body.startsWith("*")) {
                            body = body.substring(1)
                        }
                        val end = body.indexOf("*/")
                        if (end >= 0) {
                            comment.append(body, 0, end)
                            rest = body.substring(end + 2)
                        } else {
                            comment.append(body)
                            rest = ""
                            inBlock = true
                        }
                    } else {
                        rest = ""
                    }
                }

                val text = comment.toString()
                if (text.isBlank()) return@forEachIndexed

                val location = "${file.relativeTo(projectDir)}:${index + 1}"

                text.firstOrNull { it.code > 127 }?.let { offender ->
                    problems += "$location uses the non ASCII character '$offender'"
                }
                markdownInComments.forEach { (token, label) ->
                    if (text.contains(token)) {
                        problems += "$location uses $label in a comment"
                    }
                }
            }
        }

        if (problems.isNotEmpty()) {
            throw GradleException(
                "Comment style violations:\n" + problems.joinToString("\n") { "  $it" },
            )
        }
    }
}

tasks.named("check") {
    dependsOn(checkCommentStyle)
}
