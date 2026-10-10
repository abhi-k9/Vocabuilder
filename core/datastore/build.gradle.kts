plugins {
    alias(libs.plugins.vocabuilder.android.library)
    alias(libs.plugins.vocabuilder.hilt)
}

android {
    namespace = "io.github.abhik9.vocabuilder.core.datastore"
}

dependencies {
    api(libs.androidx.dataStore.preferences)
    api(projects.core.model)
    implementation(projects.core.common)

    testImplementation(libs.kotlinx.coroutines.test)
}
