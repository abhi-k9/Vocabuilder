plugins {
    alias(libs.plugins.vocabuilder.android.library)
    alias(libs.plugins.vocabuilder.android.room)
    alias(libs.plugins.vocabuilder.hilt)
}

android {
    namespace = "io.github.abhik9.vocabuilder.core.database"
}

dependencies {
    api(projects.core.model)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
}
