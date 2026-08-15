plugins {
    id("openbrasfoot.kotlin-jvm")
}

/**
 * Modules applying this convention must stay free of third party runtime
 * dependencies. Only the Kotlin standard library and sibling project modules
 * are allowed, which is what keeps the simulation portable and testable.
 *
 * kotlinx-serialization is allowed because the dataset schema and the ruleset
 * are declarative value types, not I/O.
 */
val allowedRuntimeGroups = setOf(
    "org.jetbrains.kotlin",
    "org.jetbrains.kotlinx",
    "org.jetbrains",
)

val checkPureDependencies = tasks.register("checkPureDependencies") {
    group = "verification"
    description = "Fails if this module declares a runtime dependency outside the pure allowlist."

    val moduleName = project.path
    val offenders = provider {
        configurations.getByName("runtimeClasspath")
            .resolvedConfiguration
            .lenientConfiguration
            .allModuleDependencies
            .map { it.moduleGroup to it.moduleName }
            .filter { (group, _) -> group !in allowedRuntimeGroups }
            .map { (group, name) -> "$group:$name" }
            .distinct()
            .sorted()
    }

    doLast {
        val found = offenders.get()
        if (found.isNotEmpty()) {
            throw GradleException(
                "Module $moduleName applies the kotlin-pure convention but pulls in " +
                    "disallowed runtime dependencies: ${found.joinToString(", ")}. " +
                    "Move the code that needs them into a module that does not have to stay pure.",
            )
        }
    }
}

tasks.named("check") {
    dependsOn(checkPureDependencies)
}
