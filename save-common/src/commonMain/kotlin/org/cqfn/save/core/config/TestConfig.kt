/**
 * this file contains all data structures that are related to test configuraion  (save.toml)
 */

package org.cqfn.save.core.config

import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.plugin.PluginConfig

import okio.FileSystem
import okio.Path

/**
 * Configuration for a test suite, that is read from test suite configuration file (toml config)
 * @property location [Path] denoting the location of this file
 * @property parentConfig parent config in the hierarchy of configs, `null` if this config is root.
 * @property pluginConfigs list of configurations for plugins that are active in this config
 */
@Suppress("TYPE_ALIAS")
data class TestConfig(
    val location: Path,
    val parentConfig: TestConfig?,
    val pluginConfigs: MutableList<PluginConfig> = mutableListOf(),
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
     * List of child configs in the hierarchy of ConfigDetector configs, can be empty if this config is at the very bottom.
     * NB: don't move to constructor in order not to break toString into infinite recursion.
     */
    val childConfigs: MutableList<TestConfig> = mutableListOf()

    /**
     * Directory containing [location] of this config
     */
    val directory: Path = location.parent!!

    init {
        parentConfig?.let {
            logDebug("Add child ${this.location} for ${parentConfig.location}")
            parentConfig.childConfigs.add(this)
        }
        require(fs.metadata(location).isRegularFile) {
            "Location <${location.name}> denotes a directory, but TestConfig should be created from a file"
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
    fun parentConfigs(withSelf: Boolean = false) =
            generateSequence(if (withSelf) this else parentConfig) { it.parentConfig }

    /**
     * recursively (till leafs) return all configs from the configuration Tree
     *
     */
    @Suppress("WRONG_NEWLINES")
    fun getAllTestConfigs(): List<TestConfig> {
        return listOf(this) + this.childConfigs.flatMap { it.getAllTestConfigs() }
    }

    /**
     * filtering out general configs
     */
    fun pluginConfigsWithoutGeneralConfig() = pluginConfigs.filterNot { it is GeneralConfig }

    /**
     * Merge parent list of plugins with the current list
     *
     * @return merged test config
     */
    fun mergeConfigWithParents(): TestConfig {
        logDebug("Start merging configs for ${this.location}")

        this.parentConfigs().toList().forEach { parentConfig ->
            // return from the function if we stay at the root element of the plugin tree
            val parentalPlugins = parentConfig.pluginConfigs
            parentalPlugins.forEach { currentConfig ->
                val childConfigs = this.pluginConfigs.filter { it.type == currentConfig.type }
                if (childConfigs.isEmpty()) {
                    // if we haven't found a plugin from parent in a current list of plugins - we will simply copy it
                    this.pluginConfigs.add(currentConfig)
                } else {
                    // else, we will merge plugin with a corresponding plugin from a parent config
                    // we expect that there is only one plugin of such type, otherwise we will throw an exception
                    val mergedConfig = childConfigs.single().mergeWith(currentConfig)
                    this.pluginConfigs.set(this.pluginConfigs.indexOf(childConfigs.single()), mergedConfig)
                }
            }
        }
        return this
    }

    fun validate() {
        for (config in this.pluginConfigs) {
            this.pluginConfigs.set(this.pluginConfigs.indexOf(config), config.validate())
        }
    }
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
