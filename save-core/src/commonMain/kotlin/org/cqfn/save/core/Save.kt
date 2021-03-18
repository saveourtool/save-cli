package org.cqfn.save.core

import org.cqfn.save.core.config.SaveConfig
import org.cqfn.save.core.plugin.Plugin

import okio.ExperimentalFileSystem
import okio.FileSystem

/**
 * @property saveConfig an instance of [SaveConfig]
 */
@Suppress("INLINE_CLASS_CAN_BE_USED")  // todo: remove when there are >1 constructor parameters
class Save(
    val saveConfig: SaveConfig
) {
    /**
     * Main entrypoint for SAVE framework. Discovers plugins and calls their execution.
     */
    @OptIn(ExperimentalFileSystem::class)
    fun performAnalysis() {
        // get all toml configs in file system
        val fs = FileSystem.SYSTEM
        val configFileLines: List<String> = fs.read(saveConfig.configPath) {
            generateSequence { readUtf8Line() }.toList()
        }

        val plugins: List<Plugin> = emptyList()  // todo: discover plugins
        plugins.forEach {
            it.execute(configFileLines)
        }
    }
}
