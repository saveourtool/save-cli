package org.cqfn.save.plugins.fix

import org.cqfn.save.core.plugin.PluginConfig

import okio.Path

import kotlinx.serialization.Serializable

/**
 * @property execCmd a command that will be executed to mutate test file contents
 * @property destinationFileSuffix [execCmd] should append this suffix to the file name after mutating it.
 */
@Serializable
data class FixPluginConfig(
    val execCmd: String,
    val destinationFileSuffix: String? = null,
) : PluginConfig {
    /**
     * Constructs a name of destination file from original file name and [destinationFileSuffix]
     *
     * @param original an original file
     * @return a name of destination file
     */
    fun destinationFileFor(original: Path): String {
        requireNotNull(destinationFileSuffix) { "This method should never be called when destinationFileSuffix is null" }
        return original.name.run {
            substringBeforeLast(".") + destinationFileSuffix + "." + substringAfterLast(".")
        }
    }

    companion object {
        internal val defaultResourceNamePattern = Regex("""(.+)(Expected|Test)\.[\w\d]+""")
    }
}
