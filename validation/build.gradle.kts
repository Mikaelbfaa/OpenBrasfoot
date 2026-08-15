plugins {
    id("openbrasfoot.kotlin-jvm")
    id("openbrasfoot.quality")
}

dependencies {
    implementation(project(":engine"))
    implementation(project(":dataset"))
}
