package org.cqfn.save.core

import okio.ExperimentalFileSystem
import okio.FileSystem
import okio.Path.Companion.toPath
import org.cqfn.save.core.config.SaveConfig
import org.cqfn.save.core.plugin.Plugin

class Save(
    val saveConfig: SaveConfig
) {
    @OptIn(ExperimentalFileSystem::class)
    fun performAnalysis() {
        // get all toml configs in file system
        val fs = FileSystem.SYSTEM
        val configFileLines: List<String> = fs.read(saveConfig.configPath) {
            generateSequence { readUtf8Line() }.toList()
        }

        val plugins = emptyList<Plugin>()  // todo: discover plugins
        plugins.forEach {
            it.execute(configFileLines)
        }
    }
}
