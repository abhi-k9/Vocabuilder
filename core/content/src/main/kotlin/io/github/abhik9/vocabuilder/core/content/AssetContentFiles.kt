package io.github.abhik9.vocabuilder.core.content

import android.content.res.AssetManager
import java.io.FileNotFoundException

/** The content bundled in the APK, under `assets/content`. */
class AssetContentFiles(private val assets: AssetManager) : ContentFiles {
    override fun read(path: String): String? = try {
        assets.open("$ROOT/$path").bufferedReader().use { it.readText() }
    } catch (_: FileNotFoundException) {
        null
    }

    private companion object {
        const val ROOT = "content"
    }
}
