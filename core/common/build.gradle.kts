plugins {
    alias(libs.plugins.vocabuilder.jvm.library)
    alias(libs.plugins.vocabuilder.hilt)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
}
