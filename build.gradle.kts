plugins {
    kotlin("jvm") version "1.9.21"
}

allprojects{
    repositories {
        mavenCentral()
    }
}

kotlin {
    jvmToolchain(17)
}