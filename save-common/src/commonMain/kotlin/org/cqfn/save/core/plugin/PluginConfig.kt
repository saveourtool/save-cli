/**
 * Configuration classes for SAVE plugins.
 */

package org.cqfn.save.core.plugin

import kotlinx.serialization.Serializable

/**
 * Core interface for plugin configuration
 */
interface PluginConfig

/**
 * General configuration for test suite.
 */
@Serializable
data class GeneralConfig(
    // FixMe: after ktoml will support lists we should change it
    val tags: String,
    val description: String,
    val suiteName: String,
    // FixMe: after ktoml will support lists we should change it
    val excludedTests: String = "",
    // FixMe: after ktoml will support lists we should change it
    val includedTests: String = "",
) : PluginConfig
