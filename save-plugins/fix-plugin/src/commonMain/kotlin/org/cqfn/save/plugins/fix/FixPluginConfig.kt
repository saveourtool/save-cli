package org.cqfn.save.plugins.fix

import org.cqfn.save.core.plugin.PluginConfig

import okio.Path

/**
 * @property execCmd a command that will be executed to mutate test file contents
 * @property inPlace whether the [execCmd] mutates the file in-place
 * @property destinationFileSuffix [execCmd] should append this suffix to the file name after mutating it.
 * Required when `inPlace` is `false`, not used otherwise.
 * @property resourceNamePattern pattern by which test resources will be selected
 */
data class FixPluginConfig(
    val execCmd: String,
    val inPlace: Boolean = false,
    val destinationFileSuffix: String? = null,
    override val resourceNamePattern: Regex = Regex("""(.+)(Expected|Test)\.[\w\d]+"""),
) : PluginConfig {
    init {
        require(inPlace || destinationFileSuffix != null) {
            "Plugin ${FixPlugin::class.simpleName} should either be configured with inPlace=true or have destinationFileSuffix"
        }
    }

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
}
