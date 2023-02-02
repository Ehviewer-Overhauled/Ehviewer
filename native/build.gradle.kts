plugins {
    kotlin("multiplatform")
}

kotlin {
    androidNativeArm64("native") {
        binaries {
            sharedLib("ehviewer-kotlin-native")
        }
    }
}
