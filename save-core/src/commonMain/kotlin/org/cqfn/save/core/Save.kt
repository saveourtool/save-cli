package org.cqfn.save.core

import okio.ExperimentalFileSystem
import okio.FileSystem
import okio.Path.Companion.toPath
import org.cqfn.save.core.config.SaveConfig

class Save(
    val saveConfig: SaveConfig
) {
    @OptIn(ExperimentalFileSystem::class)
    fun performAnalysis() {
        // get all toml configs in file system
        val pwd = "".toPath()
        val fs = FileSystem.SYSTEM
        fs.list(pwd).forEach {
            println(it)
        }
    }
}
