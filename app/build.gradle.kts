import com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
import com.mikepenz.aboutlibraries.plugin.DuplicateRule.GROUP
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

val isRelease: Boolean
    get() = gradle.startParameter.taskNames.any { it.contains("Release") }

plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-parcelize")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.mikepenz.aboutlibraries.plugin")
    id("org.mozilla.rust-android-gradle.rust-android")
    id("dev.shreyaspatil.compose-compiler-report-generator")
}

android {
    compileSdk = 34
    buildToolsVersion = "34.0.0"
    ndkVersion = "26.0.10404224"

    splits {
        abi {
            isEnable = true
            reset()
            if (isRelease) {
                include("arm64-v8a", "x86_64", "armeabi-v7a", "x86")
                isUniversalApk = true
            } else {
                include("arm64-v8a")
            }
        }
    }

    val signConfig = signingConfigs.create("release") {
        storeFile = File(projectDir.path + "/keystore/androidkey.jks")
        storePassword = "000000"
        keyAlias = "key0"
        keyPassword = "000000"
        enableV3Signing = true
        enableV4Signing = true
    }

    val commitSha = providers.exec {
        commandLine = "git rev-parse --short=7 HEAD".split(' ')
    }.standardOutput.asText.get().trim()

    val buildTime by lazy {
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm").withZone(ZoneOffset.UTC)
        formatter.format(Instant.now())
    }

    val repoName = providers.exec {
        commandLine = "git remote get-url origin".split(' ')
    }.standardOutput.asText.get().trim().removePrefix("https://github.com/").removePrefix("git@github.com:")
        .removeSuffix(".git")

    defaultConfig {
        applicationId = "moe.tarsin.ehviewer"
        minSdk = 28
        targetSdk = 34
        versionCode = 180042
        versionName = "1.8.9.0-SNAPSHOT"
        resourceConfigurations.addAll(
            listOf(
                "zh",
                "zh-rCN",
                "zh-rHK",
                "zh-rTW",
                "es",
                "ja",
                "ko",
                "fr",
                "de",
                "th",
                "tr",
                "nb-rNO",
            ),
        )
        buildConfigField("String", "COMMIT_SHA", "\"$commitSha\"")
        buildConfigField("String", "BUILD_TIME", "\"$buildTime\"")
        buildConfigField("String", "REPO_NAME", "\"$repoName\"")
    }

    externalNativeBuild {
        cmake {
            path = File("src/main/cpp/CMakeLists.txt")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf(
            // https://kotlinlang.org/docs/compiler-reference.html#progressive
            "-progressive",
            "-Xjvm-default=all",
            "-Xlambdas=indy",
            "-XXLanguage:+BreakContinueInInlineLambdas",

            "-opt-in=coil.annotation.ExperimentalCoilApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.paging.ExperimentalPagingApi",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
            "-opt-in=splitties.experimental.ExperimentalSplittiesApi",
            "-opt-in=splitties.preferences.DataStorePreferencesPreview",
        )
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
        disable.add("MissingTranslation")
    }

    packaging {
        resources {
            excludes += "/META-INF/**"
            excludes += "/kotlin/**"
            excludes += "**.txt"
            excludes += "**.bin"
        }
    }

    dependenciesInfo.includeInApk = false

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            signingConfig = signConfig
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    namespace = "com.hippo.ehviewer"
}

dependencies {
    // https://developer.android.com/jetpack/androidx/releases/activity
    implementation("androidx.activity:activity-compose:1.8.0-alpha06")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.biometric:biometric-ktx:1.2.0-alpha05")
    implementation("androidx.browser:browser:1.5.0")
    implementation("androidx.collection:collection-ktx:1.3.0-beta01")

    // https://developer.android.com/jetpack/androidx/releases/compose-material3
    // api(platform("androidx.compose:compose-bom:2023.05.00"))
    api(platform("dev.chrisbanes.compose:compose-bom:2023.07.00-alpha02"))
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.ui:ui-util")

    implementation("androidx.core:core-ktx:1.12.0-rc01")

    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.0-alpha12")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.fragment:fragment-ktx:1.7.0-alpha02")
    // https://developer.android.com/jetpack/androidx/releases/lifecycle
    implementation("androidx.lifecycle:lifecycle-process:2.6.1")

    // https://developer.android.com/jetpack/androidx/releases/navigation
    val nav_version = "2.7.0-rc01"
    implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation("androidx.navigation:navigation-ui-ktx:$nav_version")
    implementation("androidx.navigation:navigation-compose:$nav_version")

    // https://developer.android.com/jetpack/androidx/releases/paging
    val paging = "3.2.0"
    implementation("androidx.paging:paging-compose:$paging")
    implementation("androidx.paging:paging-runtime-ktx:$paging")
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    implementation("androidx.recyclerview:recyclerview-selection:1.2.0-alpha01")

    // https://developer.android.com/jetpack/androidx/releases/room
    val room_version = "2.6.0-alpha02"
    ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-paging:$room_version")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("com.drakeet.drawer:drawer:1.0.3")
    implementation("com.github.chrisbanes:PhotoView:2.3.0") // Dead Dependency
    implementation("com.github.tachiyomiorg:DirectionalViewPager:1.0.0") // Dead Dependency
    // https://github.com/google/accompanist/releases
    val accompanist_version = "0.31.6-rc"
    implementation("com.google.accompanist:accompanist-themeadapter-material3:$accompanist_version")
    implementation("com.google.accompanist:accompanist-webview:$accompanist_version")
    implementation("com.google.android.material:material:1.11.0-alpha01")

    val splitties = "3.0.0"
    implementation("com.louiscad.splitties:splitties-appctx:$splitties")
    implementation("com.louiscad.splitties:splitties-systemservices:$splitties")
    implementation("com.louiscad.splitties:splitties-preferences:$splitties")
    implementation("com.louiscad.splitties:splitties-arch-room:$splitties")

    // https://square.github.io/okhttp/changelogs/changelog/
    implementation(platform("com.squareup.okhttp3:okhttp-bom:5.0.0-alpha.11"))
    implementation("com.squareup.okhttp3:okhttp-android")
    implementation("com.squareup.okhttp3:okhttp-coroutines")

    implementation("com.squareup.okio:okio-jvm:3.5.0")

    implementation("com.mikepenz:aboutlibraries-core:10.8.3")

    implementation("dev.chrisbanes.insetter:insetter:0.6.1") // Dead Dependency
    implementation("dev.rikka.rikkax.core:core-ktx:1.4.1")
    implementation("dev.rikka.rikkax.insets:insets:1.3.0")
    implementation("dev.rikka.rikkax.layoutinflater:layoutinflater:1.3.0")

    implementation(platform("io.arrow-kt:arrow-stack:1.2.0"))
    implementation("io.arrow-kt:arrow-fx-coroutines")

    // https://coil-kt.github.io/coil/changelog/
    implementation(platform("io.coil-kt:coil-bom:2.4.0"))
    implementation("io.coil-kt:coil-compose")
    implementation("io.coil-kt:coil-gif")

    val ktor = "2.3.3"
    implementation("io.ktor:ktor-io-jvm:$ktor")
    implementation("io.ktor:ktor-utils-jvm:$ktor")

    implementation("org.chromium.net:cronet-embedded:113.5672.61")

    val serialization = "1.5.1"
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:$serialization")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-okio:$serialization")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jsoup:jsoup:1.16.1")

    val chunker = "4.0.0"
    debugImplementation("com.github.chuckerteam.chucker:library:$chunker")
    releaseImplementation("com.github.chuckerteam.chucker:library-no-op:$chunker")

    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

aboutLibraries {
    duplicationMode = MERGE
    duplicationRule = GROUP
    exclusionPatterns = listOf(
        "androidx\\..*".toPattern(),
        ".*annotations".toPattern(),
        "com\\.google\\.code\\..*".toPattern(),
        "com\\.google\\.guava:listenablefuture".toPattern(),
        "com\\.google\\.protobuf:protobuf-javalite".toPattern(),
        "org\\.checkerframework:checker-qual".toPattern(),
        "org\\.chromium\\.net:cronet-embedded".toPattern(),
        "org\\.slf4j:slf4j-api".toPattern(),
    )
}

cargo {
    module = "src/main/rust"
    libname = "ehviewer_rust"
    targets = if (isRelease) listOf("arm", "x86", "arm64", "x86_64") else listOf("arm64")
    if (isRelease) profile = "release"
}

tasks.configureEach {
    if ((name == "mergeDebugJniLibFolders" || name == "mergeReleaseJniLibFolders")) {
        dependsOn("cargoBuild")
        // fix mergeDebugJniLibFolders  UP-TO-DATE
        inputs.dir(buildDir.resolve("rustJniLibs/android"))
    }
}
