plugins {
    alias(libs.plugins.vocabuilder.android.library)
    alias(libs.plugins.vocabuilder.hilt)
}

android {
    namespace = "io.github.abhik9.vocabuilder.core.speech"
}

dependencies {
    implementation(projects.core.domain)
}
