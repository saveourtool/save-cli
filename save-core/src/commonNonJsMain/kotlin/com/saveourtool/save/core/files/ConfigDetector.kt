package com.saveourtool.save.core.files

import com.saveourtool.save.core.config.TestConfig
import com.saveourtool.save.core.config.isSaveTomlConfig
import com.saveourtool.save.core.logging.logDebug
import com.saveourtool.save.core.logging.logError
import com.saveourtool.save.core.logging.logTrace
import com.saveourtool.save.core.plugin.PluginConfig

import okio.FileSystem
import okio.Path

/**
 * A class that is capable of discovering config files hierarchy.
 */
class ConfigDetector(
    private val fs: FileSystem,
    private val overridesPluginConfigs: List<PluginConfig>,
) {
    /**
     * Try to create SAVE config file from [testConfig].
     *
     * @param testConfig - testing configuration (save.toml) from which SAVE config file should be built
     * @return [TestConfig] or null if no suitable config file has been found.
     * @throws IllegalArgumentException - in case of invalid testConfig file
     */
    fun configFromFile(testConfig: Path): TestConfig {
        logTrace("Discovering parent configs of $testConfig")
        // testConfig is validated in the beginning and cannot be null
        return discoverConfigWithParents(testConfig)
            // After `discoverConfigWithParents` we successfully created TestConfig instances for all save.toml files
            // starting from the given [testConfig] to the top-level save.toml in file tree.
            // Now do the same for children configs
            ?.also { config ->
                // Go down through the file tree and
                // discover all descendant configs of [config]
                logTrace("Discovering all descendant `save.toml`s of $testConfig")
                val descendantConfigLocations = config
                    .directory
                    .findAllFilesMatching { it.isSaveTomlConfig() }
                    .flatten()
                logTrace("Discovered ${descendantConfigLocations.size} files")

                createTestConfigs(descendantConfigLocations, mutableListOf(config))
            }
            ?: run {
                logError("Config file was not found in $testConfig")
                throw IllegalArgumentException("Provided directory [$testConfig] doesn't contain" +
                        " save.toml configuration file")
            }
    }

    private fun createTestConfigs(descendantConfigLocations: List<Path>, configs: MutableList<TestConfig>) =
            descendantConfigLocations
                .drop(1)  // because [config] will be discovered too
                .forEachIndexed { index, path ->
                    val parentConfig = configs.find { config ->
                        config.location ==
                                descendantConfigLocations.take(index + 1)
                                    .reversed()
                                    .find { it.parent in path.parents() }!!
                    }!!

                    configs.add(
                        TestConfig(
                            path,
                            parentConfig,
                            overridesPluginConfigs = overridesPluginConfigs,
                            fs = fs,
                        )
                    )
                }

    /**
     * Depends to type of entry point, start to create the hierarchy of TestConfig's, starting from the [file] till the top in the file tree
     *
     * @param file entry point from which SAVE should create hierarchy of [TestConfig]'s.
     * @return [TestConfig] or null if no suitable config file has been found.
     */
    private fun discoverConfigWithParents(file: Path): TestConfig? = when {
        // if provided file is a directory, try to find save.toml inside it
        fs.metadata(file).isDirectory -> getTestConfigFromDirectory(file)
        // if provided file is save.toml, create config from it
        file.isSaveTomlConfig() -> getTestConfigFromTomlFile(file)
        // if provided file is an individual test file, we search a config file in this and parent directories
        // and start processing for this single test (in case it is really a valid test file)
        else -> getTestConfigFromSingleTestFile(file)
    }

    private fun getTestConfigFromSingleTestFile(file: Path) = file
        .parents()
        .mapNotNull { directory -> directory.findChildByOrNull { it.isSaveTomlConfig() } }
        .firstOrNull()
        ?.let { discoverConfigWithParents(it) }
        .also { logDebug("Processing test config for a single test file: $file") }

    private fun getTestConfigFromDirectory(file: Path) = file
        .findChildByOrNull { it.isSaveTomlConfig() }
        ?.let { discoverConfigWithParents(it) }
        .also { logDebug("Processing test config from directory: $file") }

    private fun getTestConfigFromTomlFile(file: Path): TestConfig {
        // Create instances for all parent configs recursively
        val parentConfig = file.parents()
            .drop(1)  // because immediate parent already contains [this] config
            .mapNotNull { parentDir ->
                parentDir.findChildByOrNull {
                    it.isSaveTomlConfig()
                }
            }
            .firstOrNull()
            ?.let { getTestConfigFromTomlFile(it) }
        // Now process current config
        logDebug("Processing test config from the toml file: $file")

        return TestConfig(
            file,
            parentConfig,
            overridesPluginConfigs = overridesPluginConfigs,
            fs = fs,
        )
    }
}
