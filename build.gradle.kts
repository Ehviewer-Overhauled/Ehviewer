plugins {
    id("com.android.application") version "8.1.0-beta04" apply false
    kotlin("android") version "1.9.0-Beta" apply false
    kotlin("plugin.serialization") version "1.9.0-Beta" apply false
    id("com.google.devtools.ksp") version "1.9.0-Beta-1.0.11" apply false
    id("com.mikepenz.aboutlibraries.plugin") version "10.7.0" apply false
}

allprojects {
    apply(from = "$rootDir/ktlint.gradle.kts")
}

tasks.register("Delete", Delete::class) {
    delete(rootProject.buildDir)
}

buildscript {
    dependencies {
        classpath("com.android.tools:r8:8.2.7-dev")
    }
}
