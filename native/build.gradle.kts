import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.KonanTarget.*

plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

val jniLibDir = File(project.buildDir, arrayOf("generated", "jniLibs").joinToString(File.separator))

val sharedLib_name_prefix = "ehviewer_kotlin_native"

kotlin {
    android()

    val nativeConfigure: KotlinNativeTarget.() -> Unit = {
        binaries {
            sharedLib(sharedLib_name_prefix) {
                linkTask.doLast {
                    copy {
                        from(outputFile)

                        val typeName = if (buildType == NativeBuildType.DEBUG) "Debug" else "Release"
                        val abiDirName = when(target.konanTarget) {
                            ANDROID_ARM32 -> "armeabi-v7a"
                            ANDROID_ARM64 -> "arm64-v8a"
                            ANDROID_X86 -> "x86"
                            ANDROID_X64 -> "x86_64"
                            else -> "unknown"
                        }

                        into(file("$jniLibDir/$typeName/$abiDirName"))
                    }
                }

                afterEvaluate {
                    val preBuild by tasks.getting
                    preBuild.dependsOn(linkTask)
                }
            }
        }
    }

    androidNativeArm32(configure = nativeConfigure)
    androidNativeArm64(configure = nativeConfigure)
    androidNativeX86(configure = nativeConfigure)
    androidNativeX64(configure = nativeConfigure)

    sourceSets {
        val androidNativeArm32Main by getting
        val androidNativeArm64Main by getting
        val androidNativeX86Main by getting
        val androidNativeX64Main by getting

        val nativeMain by creating {
            androidNativeArm32Main.dependsOn(this)
            androidNativeArm64Main.dependsOn(this)
            androidNativeX86Main.dependsOn(this)
            androidNativeX64Main.dependsOn(this)
        }
    }

}

android {
    compileSdk = 33
    buildToolsVersion = "33.0.1"

    sourceSets {
        getByName("debug").jniLibs.srcDirs("$jniLibDir/Debug")
        getByName("release").jniLibs.srcDirs("$jniLibDir/Release")
    }
    namespace = "moe.tarsin.ehviewer"
}

repositories {
    mavenCentral()
    google()
}
