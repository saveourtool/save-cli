/**
 * this file contains all data structures that are related to test configuraion  (save.toml)
 */

package org.cqfn.save.core.config

import org.cqfn.save.core.plugin.PluginConfig

import okio.FileSystem
import okio.Path

/**
 * Configuration for a test suite, that is read from test suite configuration file (toml config)
 * @property location [Path] denoting the location of this file
 * @property parentConfig parent config in the hierarchy of configs, `null` if this config is root.
 * @property pluginConfigs list of configurations for plugins that are active in this config
 */
data class TestConfig(
    val location: Path,
    val parentConfig: TestConfig?,
    val pluginConfigs: List<PluginConfig> = emptyList(),
    private val fs: FileSystem = FileSystem.SYSTEM,
) {
    /**
     * Getting all neighbour configs to the current config (i.e. all configs with the same parent config)
     * - parentConfig
     * -- currentConfig
     * -- neighbourConfig
     */
    val neighbourConfigs: MutableList<TestConfig>? = this.parentConfig?.childConfigs

    /**
     * List of child configs in the hierarchy oConfigDetectorf configs, can be empty if this config is at the very bottom.
     * NB: don't move to constructor in order not to break toString into infinite recursion.
     */
    val childConfigs: MutableList<TestConfig> = mutableListOf()

    /**
     * Directory containing [location] of this config
     */
    val directory: Path = location.parent!!

    init {
        require(fs.metadata(location).isRegularFile) {
            "Location ${location.name} denotes a directory, but TestConfig should be created from a file"
        }
    }

    /**
     * @return whether this config file is in the root on the hierarchy
     */
    fun isRoot() = parentConfig == null

    /**
     * @param withSelf if true, include this config as the first element of the sequence or start with parent config otherwise
     * @return a [Sequence] of parent config files
     */
    fun parentConfigs(withSelf: Boolean = false) = generateSequence(if (withSelf) this else parentConfig) { it.parentConfig }
}

/**
 * Sections of a toml configuration for tests (including standard plugins)
 */
enum class TestConfigSections {
    FIX, GENERAL, WARN;
}

/**
 * @return whether a file denoted by this [Path] is a default save configuration file (save.toml)
 */
fun Path.isSaveTomlConfig() = name == "save.toml"
