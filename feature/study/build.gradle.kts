plugins {
    alias(libs.plugins.vocabuilder.android.feature)
}

android {
    namespace = "io.github.abhik9.vocabuilder.feature.study"
}

dependencies {
    // The flashcard's gestures are tested with Compose's test rule, on Robolectric.
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
