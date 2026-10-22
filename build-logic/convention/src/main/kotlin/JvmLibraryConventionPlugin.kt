import io.github.abhik9.vocabuilder.buildlogic.configureKotlinJvm
import io.github.abhik9.vocabuilder.buildlogic.configureSpotless
import io.github.abhik9.vocabuilder.buildlogic.configureTests
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

/** A pure Kotlin/JVM module with no Android dependencies, e.g. the domain layer. */
abstract class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.jvm")

            configureKotlinJvm()
            configureSpotless()
            configureTests()
        }
    }
}
