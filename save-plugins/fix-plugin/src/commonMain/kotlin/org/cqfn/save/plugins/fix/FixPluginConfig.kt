package org.cqfn.save.plugins.fix

import org.cqfn.save.core.plugin.PluginConfig

import okio.Path

import kotlinx.serialization.Serializable

/**
 * Some fields by default are null, instead of some natural value, because of the fact, that in stage of merging
 * of nested configs, we can't detect whether the value are passed by user, or taken from default.
 * The logic of the default value processing will be provided in stage of validation
 *
 * @property execCmd a command that will be executed to mutate test file contents
 * @property destinationFileSuffix [execCmd] should append this suffix to the file name after mutating it.
 */
@Serializable
data class FixPluginConfig(
    val execCmd: String,
    val destinationFileSuffix: String? = null,
) : PluginConfig<FixPluginConfig> {
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

    @Suppress("TYPE_ALIAS")
    override fun mergeConfigInto(childConfig: MutableList<PluginConfig<*>>) {
        val childFixConfig = childConfig.filterIsInstance<FixPluginConfig>().firstOrNull()
        val newChildFixConfig = childFixConfig?.mergePluginConfig(this) ?: this
        // Now we update child config in place
        childFixConfig?.let {
            childConfig.set(childConfig.indexOf(childFixConfig), newChildFixConfig)
        } ?: childConfig.add(newChildFixConfig)
    }

    override fun mergePluginConfig(parentConfig: FixPluginConfig) = FixPluginConfig(
        this.execCmd ?: parentConfig.execCmd,
        this.destinationFileSuffix ?: parentConfig.destinationFileSuffix
    )

    companion object {
        internal val defaultResourceNamePattern = Regex("""(.+)(Expected|Test)\.[\w\d]+""")
    }
}
