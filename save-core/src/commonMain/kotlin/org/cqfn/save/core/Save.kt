package org.cqfn.save.core

import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.files.ConfigDetector
import org.cqfn.save.core.plugin.Plugin

import okio.Path.Companion.toPath

/**
 * @property saveProperties an instance of [SaveProperties]
 */
@Suppress("INLINE_CLASS_CAN_BE_USED")  // todo: remove when there are >1 constructor parameters
class Save(
    private val saveProperties: SaveProperties
) {
    /**
     * Main entrypoint for SAVE framework. Discovers plugins and calls their execution.
     */
    fun performAnalysis() {
        // get all toml configs in file system
        val testConfig = ConfigDetector().configFromFile(saveProperties.testConfig!!.toPath())
        requireNotNull(testConfig) { "Provided path ${saveProperties.testConfig} doesn't correspond to a valid save.toml file" }

        val plugins: List<Plugin> = emptyList()  // todo: discover plugins (from configuration blocks in TestSuiteConfig?)
        plugins.forEach {
            it.execute(saveProperties, testConfig)
        }
    }
}
