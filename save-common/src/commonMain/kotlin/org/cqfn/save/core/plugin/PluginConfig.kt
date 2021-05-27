/**
 * Configuration classes for SAVE plugins.
 */

package org.cqfn.save.core.plugin

import kotlinx.serialization.Serializable

/**
 * Core interface for plugin configuration
 */
interface PluginConfig<T : PluginConfig<T>> {
    /**
     * Merge current config into [childConfig]
     *
     * @param childConfig list of child configs, which will be filtered for merging
     */
    @Suppress("TYPE_ALIAS")
    fun mergeConfigInto(childConfig: MutableList<PluginConfig<*>>)

    /**
     * Function which is capable for merging inherited configurations
     *
     * @param parentConfig config which will be merged with [this]
     * @return corresponding merged config
     */
    fun mergePluginConfig(parentConfig: T): T
}

/**
 * General configuration for test suite.
 * Some fields by default are null, instead of some natural value, because of the fact, that in stage of merging
 * of nested configs, we can't detect whether the value are passed by user, or taken from default.
 * The logic of the default value processing will be provided in stage of validation
 *
 * @property tags FixMe: after ktoml will support lists we should change it
 * @property description
 * @property suiteName
 * @property excludedTests FixMe: after ktoml will support lists we should change it
 * @property includedTests FixMe: after ktoml will support lists we should change it
 */
@Serializable
data class GeneralConfig(
    val tags: String,
    val description: String,
    val suiteName: String,
    val excludedTests: String? = null,
    val includedTests: String? = null,
) : PluginConfig<GeneralConfig> {
    @Suppress("TYPE_ALIAS")
    override fun mergeConfigInto(childConfig: MutableList<PluginConfig<*>>) {
        val childGeneralConfig = childConfig.filterIsInstance<GeneralConfig>().firstOrNull()
        val newChildGeneralConfig = childGeneralConfig?.mergePluginConfig(this) ?: this
        // Now we update child config in place
        childGeneralConfig?.let {
            childConfig.set(childConfig.indexOf(childGeneralConfig), newChildGeneralConfig)
        } ?: childConfig.add(newChildGeneralConfig)
    }

    override fun mergePluginConfig(parentConfig: GeneralConfig): GeneralConfig {
        val mergedTag = parentConfig.tags?.let {
            val parentTags = parentConfig.tags.split(", ")
            val childTags = this.tags.split(", ")
            parentTags.union(childTags).joinToString(", ")
        } ?: this.tags
        return GeneralConfig(
            mergedTag,
            this.description ?: parentConfig.description,
            this.suiteName ?: parentConfig.suiteName,
            this.excludedTests ?: parentConfig.excludedTests,
            this.includedTests ?: parentConfig.includedTests
        )
    }
}
