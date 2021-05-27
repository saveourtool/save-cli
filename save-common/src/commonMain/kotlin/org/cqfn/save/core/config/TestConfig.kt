/**
 * this file contains all data structures that are related to test configuraion  (save.toml)
 */

package org.cqfn.save.core.config

import org.cqfn.save.core.logging.logDebug
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
    val pluginConfigs: MutableList<PluginConfig<*>> = mutableListOf(),
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
    fun parentConfigs(withSelf: Boolean = false) = generateSequence(if (withSelf) this else parentConfig) { it.parentConfig }

    /**
     * recursively (till leafs) return all configs from the configuration Tree
     *
     */
    @Suppress("WRONG_NEWLINES")
    fun getAllTestConfigs(): List<TestConfig> {
        return listOf(this) + this.childConfigs.flatMap { it.getAllTestConfigs() }
    }
    
    /**
     * Merge parent configurations with current and prolong it for all child configs
     */
    fun merge() {
        logDebug("Start merging configs for ${this.location}")
        val parentConfigs = parentConfigs(withSelf = true).toList().asReversed()
        mergeConfigList(parentConfigs)
        mergeChildConfigs()
    }

    // Merge list of configs pairwise
    private fun mergeConfigList(configList: List<TestConfig>) {
        if (configList.size == 1) {
            return
        }
        val pairs = configList.zipWithNext()

        pairs.forEach { (parent, child) ->
            child.mergeChildConfigWithParent(parent)
        }
    }

    // Merge child configs recursively
    private fun mergeChildConfigs() {
        for (child in childConfigs) {
            child.mergeChildConfigWithParent(parent = this)
            child.mergeChildConfigs()
        }
    }

    private fun mergeChildConfigWithParent(parent: TestConfig) {
        logDebug("Merging ${parent.location} with ${this.location}")
        val parentPluginConfigs = parent.pluginConfigs
        val childPluginConfigs = this.pluginConfigs

        // Going through parent configs and:
        // If some config is absent in parent, but exists is child, leave it as it is
        // If some config is absent in child, but exists in parent, just take it from parent
        // Otherwise we will merge configs
        val result: MutableList<PluginConfig<*>> = mutableListOf()
        for (config in parentPluginConfigs) {
            val newPluginConfig = config.createNewPluginConfig(childPluginConfigs)
            result.add(newPluginConfig)
        }

        // Now we update child config in place
        childPluginConfigs.clear()
        result.forEach { childPluginConfigs.add(it) }
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
