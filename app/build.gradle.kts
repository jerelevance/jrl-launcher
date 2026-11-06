plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// versionCode climbs with every CI run so each new APK installs over the last one.
val runNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "com.jerelevance.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jerelevance.launcher"
        minSdk = 26
        targetSdk = 35
        versionCode = runNumber
        versionName = "0.1.$runNumber"
    }

    // One fixed key (app/jrl.keystore) so updates install over each other.
    // It's a personal sideload key, not a Play Store key.
    signingConfigs {
        create("jrl") {
            storeFile = file("jrl.keystore")
            storePassword = "jrlauncher"
            keyAlias = "jrl"
            keyPassword = "jrlauncher"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("jrl")
        }
        debug {
            signingConfig = signingConfigs.getByName("jrl")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    androidResources {
        // keep the web assets (and the fonts dir) uncompressed and intact
        noCompress += listOf("ttf", "html")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.webkit:webkit:1.12.1")
}
