plugins {
    kotlin("jvm") version "1.9.21"
}

group = "ru.tomindapps"
version = "0.0.1"

dependencies {

    implementation("com.squareup:kotlinpoet:1.12.0")
    implementation("com.squareup:kotlinpoet-ksp:1.12.0")
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.23-1.0.20")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}