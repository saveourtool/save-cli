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
    override fun mergePluginConfig(parentConfig: GeneralConfig) = GeneralConfig(
        // TODO split and merge tags
        this.tags ?: parentConfig.tags,
        this.description ?: parentConfig.description,
        this.suiteName ?: parentConfig.suiteName,
        this.excludedTests ?: parentConfig.excludedTests,
        this.includedTests ?: parentConfig.includedTests
    )
}
