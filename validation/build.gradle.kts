plugins {
    id("openfoot.kotlin-jvm")
    id("openfoot.quality")
}

dependencies {
    implementation(project(":engine"))
    implementation(project(":dataset"))
}
