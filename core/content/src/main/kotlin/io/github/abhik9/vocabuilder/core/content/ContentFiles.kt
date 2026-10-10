package io.github.abhik9.vocabuilder.core.content

/**
 * Reads the bundled content files, by their path relative to the content root, such as
 * `gre/pack.json`. Returns `null` for a file that doesn't exist.
 */
fun interface ContentFiles {
    fun read(path: String): String?
}
