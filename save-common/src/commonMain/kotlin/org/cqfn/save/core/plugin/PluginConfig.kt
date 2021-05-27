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
     * Create new config by merging of [this] and corresponding config from [childConfig]
     *
     * @param childConfig list of child configs, which will be filtered for merging
     * @return new config
     */
    @Suppress("TYPE_ALIAS")
    fun createNewPluginConfig(childConfig: MutableList<PluginConfig<*>>): T

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
    override fun createNewPluginConfig(childConfig: MutableList<PluginConfig<*>>): GeneralConfig {
        val childGeneralConfig = childConfig.filterIsInstance<GeneralConfig>().firstOrNull()
        return childGeneralConfig?.mergePluginConfig(this) ?: this
    }

    override fun mergePluginConfig(parentConfig: GeneralConfig): GeneralConfig  {
        val mergedTag = parentConfig.tags?.let{
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
