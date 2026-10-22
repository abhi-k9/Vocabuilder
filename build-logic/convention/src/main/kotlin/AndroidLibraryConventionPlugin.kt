import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import io.github.abhik9.vocabuilder.buildlogic.AndroidSdk
import io.github.abhik9.vocabuilder.buildlogic.configureKotlinAndroid
import io.github.abhik9.vocabuilder.buildlogic.configureLint
import io.github.abhik9.vocabuilder.buildlogic.configureSpotless
import io.github.abhik9.vocabuilder.buildlogic.configureTests
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

abstract class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                testOptions.targetSdk = AndroidSdk.TARGET
                testOptions.animationsDisabled = true
                testOptions.unitTests.isIncludeAndroidResources = true
                lint.targetSdk = AndroidSdk.TARGET
                lint { configureLint() }
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            extensions.configure<LibraryAndroidComponentsExtension> {
                // Only create instrumented test variants for modules that actually have them.
                beforeVariants {
                    it.androidTest.enable = it.androidTest.enable &&
                        projectDir.resolve("src/androidTest").exists()
                }
            }
            configureSpotless()
            configureTests()
        }
    }
}
