plugins {
    id("openfoot.kotlin-jvm")
    id("openfoot.quality")
    application
}

dependencies {
    implementation(project(":engine"))
    implementation(project(":dataset"))
    implementation(libs.kotlinx.serialization.json)
}

application {
    mainClass.set("org.openfoot.cli.MainKt")
    applicationName = "openfoot-cli"
}
