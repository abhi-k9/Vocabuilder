plugins {
    alias(libs.plugins.vocabuilder.android.library.compose)
}

android {
    namespace = "io.github.abhik9.vocabuilder.core.ui"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)
}
