plugins {
    alias(libs.plugins.vocabuilder.android.feature)
}

android {
    namespace = "io.github.abhik9.vocabuilder.feature.settings"
}

dependencies {
    implementation(projects.core.common)
}
