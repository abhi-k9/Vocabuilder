import java.util.Properties

plugins {
    alias(libs.plugins.vocabuilder.android.application)
    alias(libs.plugins.vocabuilder.android.application.compose)
    alias(libs.plugins.vocabuilder.hilt)
}

// Release signing reads keystore.properties (not checked in) when present; see README.
val keystoreProperties = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.inputStream().use(::load) }
}

// The release workflow passes the version from the git tag, e.g. -Pvocabuilder.versionName=1.2.3.
val appVersionName: String = providers.gradleProperty("vocabuilder.versionName").getOrElse("1.0.0")

/** Turns "1.2.3" into 10203, so every release has a higher version code than the one before. */
fun versionCodeOf(versionName: String): Int {
    val parts = versionName.split('.').map { it.toIntOrNull() }
    require(parts.size == 3 && parts.all { it != null && it in 0..99 }) {
        "The version must look like 1.2.3, with each part from 0 to 99, but was \"$versionName\"."
    }
    val (major, minor, patch) = parts.requireNoNulls()
    return major * 10_000 + minor * 100 + patch
}

android {
    namespace = "io.github.abhik9.vocabuilder"

    defaultConfig {
        applicationId = "io.github.abhik9.vocabuilder"
        versionCode = versionCodeOf(appVersionName)
        versionName = appVersionName
    }

    signingConfigs {
        if (keystoreProperties != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Without a release keystore, sign with the debug key so release builds still install.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.core.ktx)
}
