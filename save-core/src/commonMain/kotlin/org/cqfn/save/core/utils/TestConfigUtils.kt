/**
 * Utilities for work with TestConfigs
 */

package org.cqfn.save.core.utils

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.config.TestConfigSections
import org.cqfn.save.core.plugin.PluginException
import org.cqfn.save.plugin.warn.WarnPlugin
import org.cqfn.save.plugins.fix.FixPlugin

/**
 * Evaluate all descendants of [this] config, reading individual plugin configurations from TOML file.
 *
 * @return [this] config with all descendants being evaluated (`pluginConfigs` are filled with data and merged with parents)
 */
fun TestConfig.processInPlace() = processInPlace {
    createPluginConfigListFromToml(it.location)
}

/**
 * Creates a list of plugins according to [this] config, choosing plugin implementation from the list of available ones.
 *
 * @return a list of plugins. Includes only plugins with non-empty test resources.
 */
fun TestConfig.buildActivePlugins() = buildActivePlugins { pluginConfig, testConfig ->
    when (pluginConfig.type) {
        TestConfigSections.FIX -> FixPlugin(testConfig)
        TestConfigSections.WARN -> WarnPlugin(testConfig)
        else -> throw PluginException("Unknown type <${pluginConfig::class}> of plugin config was provided")
    }
}
