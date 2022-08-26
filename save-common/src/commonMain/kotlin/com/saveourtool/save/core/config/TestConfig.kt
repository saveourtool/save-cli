/**
 * this file contains all data structures that are related to test configuraion  (save.toml)
 */

package com.saveourtool.save.core.config

import com.saveourtool.save.core.files.parents
import com.saveourtool.save.core.logging.logDebug
import com.saveourtool.save.core.logging.logTrace
import com.saveourtool.save.core.plugin.GeneralConfig
import com.saveourtool.save.core.plugin.Plugin
import com.saveourtool.save.core.plugin.PluginConfig
import com.saveourtool.save.core.utils.mergeWith
import com.saveourtool.save.core.utils.overrideBy
import com.saveourtool.save.core.utils.singleIsInstanceOrNull

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

import kotlin.js.JsName

/**
 * Configuration for a test suite, that is read from test suite configuration file (toml config)
 * @property location [Path] denoting the location of this file
 * @property parentConfig parent config in the hierarchy of configs, `null` if this config is root.
 * @property pluginConfigs list of configurations for plugins that are active in this config
 * @property overridesPluginConfigs list of configurations for plugins that overrides [pluginConfigs]
 * @property fs filesystem which can access test configs
 */
@Suppress("TYPE_ALIAS", "TooManyFunctions")
data class TestConfig(
    val location: Path,
    val parentConfig: TestConfig?,
    val pluginConfigs: MutableList<PluginConfig> = mutableListOf(),
    val overridesPluginConfigs: List<PluginConfig>,
    val fs: FileSystem,
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
            logTrace("Add child ${this.location} for ${parentConfig.location}")
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
     * @return root config from hierarchy tree
     */
    fun getRootConfig() = if (!isRoot()) {
        this.parentConfigs().last()
    } else {
        this
    }

    /**
     * Find [GeneralConfig] among this config's sections
     *
     * @return [GeneralConfig] or `null` if not found
     */
    fun getGeneralConfig(): GeneralConfig? = pluginConfigs.singleIsInstanceOrNull()

    /**
     * @param withSelf if true, include this config as the first element of the sequence or start with parent config otherwise
     * @return a [Sequence] of parent config files
     */
    fun parentConfigs(withSelf: Boolean = false) =
            generateSequence(if (withSelf) this else parentConfig) { it.parentConfig }

    /**
     * recursively (till leaves) return all configs from the configuration Tree
     *
     * @return all configs from the configuration Tree
     */
    fun getAllTestConfigs(): List<TestConfig> =
            listOf(this) + this.childConfigs.flatMap { it.getAllTestConfigs() }

    /**
     * recursively return all configs from the configuration Tree, belonging to [requestedConfigs]
     *
     * @param requestedConfigs a list of save.toml configs
     * @return a list of test configs in the sub-tree with [requestedConfigs] in the inheritance tree
     */
    fun getAllTestConfigsForFiles(requestedConfigs: List<String>): List<TestConfig> =
            if (requestedConfigs.isEmpty()) {
                getAllTestConfigs()
            } else {
                getAllTestConfigs().filter { testConfig ->
                    requestedConfigs.any {
                        // `testConfig`'s directory is a parent of `requestedConfig`'s directory
                        testConfig.directory in it.toPath().parents() ||
                                // or `testConfig`'s directory is a child of `requestedConfig`'s directory
                                it.toPath().parent!! in testConfig.location.parents()
                    }
                }
            }

    /**
     * Walk all descendant configs and merge them with their parents
     *
     * @param createPluginConfigList a function which can create a list of [PluginConfig]s for this [TestConfig]
     * @return an update this [TestConfig]
     */
    fun processInPlace(createPluginConfigList: (TestConfig) -> List<PluginConfig>): TestConfig {
        // discover plugins from the test configuration
        createPluginConfigList(this).forEach { pluginConfig ->
            logTrace("Discovered new pluginConfig: $pluginConfig")
            require(this.pluginConfigs.none { it.type == pluginConfig.type }) {
                "Found duplicate for $pluginConfig"
            }
            this.pluginConfigs.add(pluginConfig)
        }
        // merge configurations with parents
        mergeConfigWithParent()
        overrideConfig()
        return this
    }

    /**
     * Construct plugins from this config and filter out those, that don't have any test resources
     *
     * @param pluginFromConfig a function which can create a list of [Plugin]s for this [TestConfig]
     * @return a list of [Plugin]s from this config with non-empty test resources
     */
    fun buildActivePlugins(pluginFromConfig: (PluginConfig, TestConfig) -> Plugin): List<Plugin> =
            pluginConfigsWithoutGeneralConfig()
                .map {
                    // create plugins from the configuration
                    pluginFromConfig(it, this)
                }
                // filter out plugins that don't have any resources
                .filter { plugin ->
                    plugin.discoverTestFiles(directory).any().also { isNotEmpty ->
                        if (!isNotEmpty) {
                            logDebug("Plugin <${plugin::class.simpleName}> in config file ${plugin.testConfig.location} has no test resources in the same directory; " +
                                    "it's config will only be used for inheritance of configuration to nested test suites")
                        }
                    }
                }

    /**
     * filtering out general configs
     *
     * @return all plugin configs without general config
     */
    private fun pluginConfigsWithoutGeneralConfig() = pluginConfigs.filterNot { it is GeneralConfig }

    /**
     * Merge parent list of plugins with the current list
     *
     * @return merged test config
     */
    private fun mergeConfigWithParent(): TestConfig {
        logDebug("Merging configs  (with parental configs from higher directory level) for ${this.location}")

        parentConfig?.let {
            logTrace("Using parental config ${parentConfig.location} to merge it with child config: ${this.location}")
            this.pluginConfigs.mergeWith(parentConfig.pluginConfigs)
        }
        return this
    }

    private fun overrideConfig() {
        logDebug("Overriding configs for $location")
        pluginConfigs.overrideBy(overridesPluginConfigs)
    }

    /**
     * Method, which validates all plugin configs and set default values, if possible
     */
    fun validateAndSetDefaults() {
        pluginConfigs.forEachIndexed { index, config ->
            pluginConfigs[index] = config.validateAndSetDefaults()
        }
        logDebug("Validated plugin configuration for [$location] " +
                "(${pluginConfigs.map { it.type }.filterNot { it == TestConfigSections.GENERAL }})")
    }
}

/**
 * Sections of a toml configuration for tests (including standard plugins)
 */
@Suppress("EnumNaming", "BACKTICKS_PROHIBITED")
enum class TestConfigSections {
    FIX,
    GENERAL,
    WARN,

    // fixme: if we will read TOML configs in JS, we'll need ability to use name w/p spaces in JS too.
    // This is illegal for JS identifier name, but can be done by adding a new field to this class.
    @JsName("FIX_AND_WARN") `FIX AND WARN`,
    ;
}

/**
 * @return whether a file denoted by this [Path] is a default save configuration file (save.toml)
 */
fun Path.isSaveTomlConfig() = name == "save.toml"

/**
 * @return a file (save.toml) in current directory
 */
fun Path.resolveSaveTomlConfig() = this / "save.toml"

/**
 * @return a file (save-overrides.toml) in current directory
 */
fun Path.resolveSaveOverridesTomlConfig() = this / "save-overrides.toml"
