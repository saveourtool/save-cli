package org.cqfn.save.plugins.fix

import org.cqfn.save.core.plugin.PluginConfig

import okio.Path

/**
 * @property execCmd a command that will be executed to mutate test file contents
 * @property testResources list of paths to test resources
 */
data class FixPluginConfig(
    val execCmd: String,
    val testResources: List<Path> = emptyList(),
) : PluginConfig
