plugins {
    kotlin("jvm") version "1.9.21"
    id("com.google.devtools.ksp") version "1.9.21-1.0.16"
}

group = "ru.tomindapps"

dependencies {
    ksp(project(":entityit"))

    implementation(project(":entityit"))
}
