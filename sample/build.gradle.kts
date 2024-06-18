plugins {
    kotlin("jvm") version "1.9.21"
    id("com.google.devtools.ksp") version "1.9.21-1.0.16"
}

group = "io.github.yushman"

dependencies {
    ksp(project(":entityit"))

    implementation(project(":entityit"))
}
