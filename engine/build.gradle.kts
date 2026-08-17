plugins {
    id("openfoot.kotlin-pure")
    id("openfoot.quality")
}

dependencies {
    api(project(":model"))
    api(project(":dataset"))
}
