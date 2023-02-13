plugins {
    id("com.android.application") version "8.0.0-beta02" apply false
    kotlin("android") version "1.8.10" apply false
    kotlin("plugin.serialization") version "1.8.10" apply false
    id("com.google.devtools.ksp") version "1.8.10-1.0.9" apply false
}

tasks.register("Delete", Delete::class) {
    delete(rootProject.buildDir)
}
