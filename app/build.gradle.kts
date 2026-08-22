plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.tristinbaker.defide"

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tristinbaker.defide"
        minSdk = 26
        targetSdk = 35
        versionCode = 27
        versionName = "2.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("debugKey") {
            storeFile = file("${rootProject.projectDir}/.android/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            storeFile = file("${System.getProperty("user.home")}/.android/defide-release.jks")
            storePassword = System.getenv("DEFIDE_STORE_PASSWORD") ?: ""
            keyAlias = "defide"
            keyPassword = System.getenv("DEFIDE_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debugKey")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // Perf-test build: release optimizations, signed with debug key so no production keystore needed.
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debugKey")
            isMinifyEnabled = true
            isDebuggable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

// ---------------------------------------------------------------------------
// Pre-build: compile content DB from source JSON + DivinumOfficium submodule
// Run with: ./gradlew compileContent
// Automatically runs before assembleDebug / assembleRelease
// ---------------------------------------------------------------------------
val compileContent by tasks.registering {
    group = "build"
    description = "Compile content databases from source JSON and DivinumOfficium submodule"

    doLast {
        val script = rootProject.file("scripts/compile_content.py")
        if (!script.exists()) {
            logger.warn("compile_content.py not found — skipping content compilation")
            return@doLast
        }

        val dbFile = rootProject.file("app/src/main/assets/databases/defide_content.db")
        val submoduleDir = rootProject.file("divinum-officium")
        if (!submoduleDir.exists() || !submoduleDir.resolve("web").exists()) {
            logger.warn(
                "DivinumOfficium submodule not found at divinum-officium/. " +
                "Run: git submodule update --init --recursive"
            )
        }

        val result = exec {
            commandLine("python3", script.absolutePath)
            workingDir(rootProject.projectDir)
            isIgnoreExitValue = false
        }
        if (result.exitValue != 0) {
            throw GradleException("compile_content.py failed with exit code ${result.exitValue}")
        }

        if (!dbFile.exists()) {
            throw GradleException(
                "compile_content.py ran but did not produce $dbFile. " +
                "Check scripts/compile_content.py output for errors."
            )
        }
        logger.lifecycle("Content DB compiled: ${dbFile.length() / 1024 / 1024} MB")
    }
}

// Register compileContent as a dependency of all assemble variants
android.applicationVariants.all {
    tasks.getByName("assemble${name.replaceFirstChar { it.uppercase() }}") {
        dependsOn(compileContent)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // Biometric (app lock)
    implementation(libs.androidx.biometric)

    // Glance (home screen widget)
    implementation(libs.glance.appwidget)

    // Coil (image loading from bundled assets)
    implementation(libs.coil.compose)
    // WebView (for in-app browser)
    implementation(libs.androidx.browser)

    // WorkManager
    implementation(libs.workmanager)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.mockk.android)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
