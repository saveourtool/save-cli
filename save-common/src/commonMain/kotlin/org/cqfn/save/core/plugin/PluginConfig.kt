/**
 * Configuration classes for SAVE plugins.
 */

package org.cqfn.save.core.plugin

import kotlinx.serialization.Serializable

import kotlin.jvm.JvmInline

/**
 * Core interface for plugin configuration
 */
interface PluginConfig

/**
 * General configuration for test suite.
 * @property tags FixMe: after ktoml will support lists we should change it
 * @property description
 * @property suiteName
 * @property excludedTests FixMe: after ktoml will support lists we should change it
 * @property includedTests FixMe: after ktoml will support lists we should change it
 * @property ignoreSaveComments if true then ignore warning comments
 */
@Serializable
@JvmInline
data class GeneralConfig(
    val tags: String,
    val description: String,
    val suiteName: String,
    val excludedTests: String = "",
    val includedTests: String = "",
    val ignoreSaveComments: Boolean = false,
) : PluginConfig
