plugins {
    id("openfoot.kotlin-jvm")
    id("openfoot.quality")
    application
}

dependencies {
    implementation(project(":engine"))
    implementation(project(":dataset"))
}

application {
    mainClass.set("org.openfoot.cli.MainKt")
    applicationName = "openfoot-cli"
}
