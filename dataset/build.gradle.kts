plugins {
    id("openfoot.kotlin-pure")
    id("openfoot.quality")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":model"))
    implementation(libs.kotlinx.serialization.json)
}
