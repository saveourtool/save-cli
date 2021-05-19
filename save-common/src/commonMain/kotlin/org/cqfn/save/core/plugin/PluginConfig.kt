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
 * @property suiteName name of the test suite
 */
@Suppress("INLINE_CLASS_CAN_BE_USED")
class GeneralConfig(
    val suiteName: String,
) : PluginConfig
