plugins {
    kotlin("jvm") version "1.9.21"
}

allprojects{
    repositories {
        mavenCentral()
        google()
    }
}


buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.21")
        classpath("com.squareup:kotlinpoet:1.12.0")
        classpath("com.squareup:kotlinpoet-ksp:1.12.0")
    }
}

kotlin {
    jvmToolchain(17)
}