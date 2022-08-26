/**
 * This file contains utils methods to validate PluginConfig
 */

package com.saveourtool.save.core.utils

import com.saveourtool.save.core.plugin.PluginConfig

/**
 * Validate [value] that [field] is not null
 *
 * @param field
 * @param value
 * @return [value] as not null
 */
fun <T : PluginConfig, R : Any> T.requireNotNull(field: String, value: R?) = requireNotNull(value) {
    """
        Error: Couldn't find `$field` in [$type] section of `$configLocation` config.
        Current configuration: ${this.currentConfiguration()}
        Please provide it in this, or at least in one of the parent configs.
    """.trimIndent()
}

/**
 * Validate [value] that [field] positive
 *
 * @param field
 * @param value
 * @return nothing
 */
fun <T : PluginConfig> T.requirePositive(field: String, value: Long) = require(value >= 0) {
    """
        [Configuration Error]: `$field` in [$type] section of `$configLocation` config should be positive!
        Current configuration: ${this.currentConfiguration()}
        """.trimIndent()
}

private fun <T : PluginConfig> T.currentConfiguration() = toString()
    .substringAfter("(")
    .substringBefore(")")
