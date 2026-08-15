plugins {
    id("openbrasfoot.kotlin-pure")
    id("openbrasfoot.quality")
}

dependencies {
    api(project(":model"))
    api(project(":dataset"))
}
