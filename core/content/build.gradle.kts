plugins {
    alias(libs.plugins.vocabuilder.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.abhik9.vocabuilder.core.content"
}

dependencies {
    api(projects.core.model)
    implementation(libs.kotlinx.serialization.json)
}
