plugins {
    alias(libs.plugins.vocabuilder.android.library)
    alias(libs.plugins.vocabuilder.hilt)
}

android {
    namespace = "io.github.abhik9.vocabuilder.core.reminders"
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.common)
    implementation(libs.androidx.core.ktx)
}
