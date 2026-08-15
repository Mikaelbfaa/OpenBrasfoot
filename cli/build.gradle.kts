plugins {
    id("openbrasfoot.kotlin-jvm")
    id("openbrasfoot.quality")
    application
}

dependencies {
    implementation(project(":engine"))
    implementation(project(":dataset"))
}

application {
    mainClass.set("org.openbrasfoot.cli.MainKt")
    applicationName = "openbrasfoot-cli"
}
