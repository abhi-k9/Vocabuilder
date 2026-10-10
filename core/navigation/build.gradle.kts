plugins {
    alias(libs.plugins.vocabuilder.android.library.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.abhik9.vocabuilder.core.navigation"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    api(projects.core.model)
    implementation(libs.kotlinx.serialization.json)
}
