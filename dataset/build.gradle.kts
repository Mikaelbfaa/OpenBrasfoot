plugins {
    id("openbrasfoot.kotlin-pure")
    id("openbrasfoot.quality")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":model"))
    implementation(libs.kotlinx.serialization.json)
}
