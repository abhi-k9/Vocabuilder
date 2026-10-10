plugins {
    alias(libs.plugins.vocabuilder.jvm.library)
    alias(libs.plugins.vocabuilder.hilt)
}

dependencies {
    api(projects.core.common)
    api(projects.core.model)

    testImplementation(projects.core.testing)
}
