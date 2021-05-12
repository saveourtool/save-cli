/**
 * Configuration classes for SAVE plugins.
 */

package org.cqfn.save.core.plugin

/**
 * Core interface for plugin configuration
 */
@Suppress("INLINE_CLASS_CAN_BE_USED", "USE_DATA_CLASS")
interface PluginConfig {
    /**
     * A regular expression to match resources for a plugin.
     * All files with names matching [resourceNamePattern] will be treated as a s single test.
     * E.g., `".+(Expected|Test)\.java"` will match pairs of files with different suffix.
     */
    val resourceNamePattern: Regex
}

/**
 * General configuration for test suite.
 */
@Suppress("INLINE_CLASS_CAN_BE_USED")
class GeneralConfig : PluginConfig {
    /**
     * This regex matches nothing, because general config doesn't have dedicated resources.
     */
    override val resourceNamePattern = Regex("a^")
}
