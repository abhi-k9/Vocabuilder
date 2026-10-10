plugins {
    alias(libs.plugins.vocabuilder.android.library)
    alias(libs.plugins.vocabuilder.hilt)
}

android {
    namespace = "io.github.abhik9.vocabuilder.core.data"
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.common)
    implementation(projects.core.content)
    implementation(projects.core.database)
    implementation(projects.core.datastore)

    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
}
