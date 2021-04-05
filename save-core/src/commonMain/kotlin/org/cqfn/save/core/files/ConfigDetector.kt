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
    fun configFromFile(file: Path): TestSuiteConfig? = when {
        // if provided file is save.toml, create config from it
        file.isDefaultSaveConfig() -> TestSuiteConfig(
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
        // if provided file is a directory, try to find save.toml inside it
        FileSystem.SYSTEM.metadata(file).isDirectory -> file
            .findChildByOrNull { it.isDefaultSaveConfig() }
            ?.let { configFromFile(it) }
        // if provided file is an individual test file, we search a config file in this and parent directories
        file.name.matches(Regex(".*Test\\.\\w+")) -> file.parents()
            .mapNotNull { dir ->
                dir.findChildByOrNull { it.isDefaultSaveConfig() }
            }
            .firstOrNull()
            ?.let { configFromFile(it) }
        else -> null
    }
}
