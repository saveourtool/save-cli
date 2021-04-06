package org.cqfn.save.core.files

import org.cqfn.save.core.config.TestSuiteConfig
import org.cqfn.save.core.config.isDefaultSaveConfig

import okio.FileSystem
import okio.Path

/**
 * A class that is capable of discovering config files hierarchy.
 */
class ConfigDetector {
    /**
     * Try to create SAVE config file from [file].
     *
     * @param file a [Path] from which SAVE config file should be built.
     * @return [TestSuiteConfig] or null if no suitable config file has been found.
     */
    fun configFromFile(file: Path): TestSuiteConfig? = discoverConfigWithParents(file)
        ?.also { config ->
            // fill children for parent configs
            config.parentConfigs().toList().reversed()
                .zipWithNext().forEach { (parent, child) ->
                    parent.childConfigs.add(child)
                }
            // discover all descendant configs of [config]
            val locationsFlattened = config.location.parent!!.findAllFilesMatching { it.isDefaultSaveConfig() }.flatten()
            val configs = mutableListOf(config)
            locationsFlattened
                .drop(1)  // because [config] will be discovered too
                .forEachIndexed { index, path ->
                    configs.add(
                        TestSuiteConfig(
                            "", "", path,
                            configs.find { discoveredConfig ->
                                discoveredConfig.location ==
                                        locationsFlattened.take(index + 1).reversed().find { it.parent in path.parents() }!!
                            }!!
                        ).also {
                            // println("Adding $it as a child")
                            it.parentConfig?.childConfigs?.add(it)
                        }
                    )
                }
        }

    private fun discoverConfigWithParents(file: Path): TestSuiteConfig? = when {
        // if provided file is a directory, try to find save.toml inside it
        FileSystem.SYSTEM.metadata(file).isDirectory -> file
            .findChildByOrNull { it.isDefaultSaveConfig() }
            ?.let { discoverConfigWithParents(it) }
        // if provided file is an individual test file, we search a config file in this and parent directories
        file.name.matches(Regex(".*Test\\.\\w+")) -> file.parents()
            .mapNotNull { dir ->
                dir.findChildByOrNull { it.isDefaultSaveConfig() }
            }
            .firstOrNull()
            ?.let { discoverConfigWithParents(it) }
        // if provided file is save.toml, create config from it
        file.isDefaultSaveConfig() -> testSuiteConfigFromFile(file)
        else -> null
    }

    private fun testSuiteConfigFromFile(file: Path) = TestSuiteConfig(
        "todo: read from file",
        "todo: read from file",
        file,
        file.parents()
            .drop(1)  // because immediate parent already contains [this] config
            .mapNotNull { parentDir ->
                parentDir.findChildByOrNull {
                    it.isDefaultSaveConfig()
                }
            }
            .firstOrNull()
            ?.let { configFromFile(it) }
    )
}
