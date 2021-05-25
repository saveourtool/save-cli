/**
 * Configuration classes for SAVE plugins.
 */

package org.cqfn.save.core.plugin

/**
 * Core interface for plugin configuration
 */
interface PluginConfig

/**
 * General configuration for test suite.
 * @property tags FixMe: after ktoml will support lists we should change it
 * @property description
 * @property excludedTests FixMe: after ktoml will support lists we should change it
 * @property includedTests FixMe: after ktoml will support lists we should change it
 */
data class GeneralConfig(
    val tags: String,
    val description: String,
    val excludedTests: String,
    val includedTests: String,
) : PluginConfig
