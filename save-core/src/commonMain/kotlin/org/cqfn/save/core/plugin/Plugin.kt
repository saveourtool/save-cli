package org.cqfn.save.core.plugin

import org.cqfn.save.core.config.TestConfig

/**
 * Plugin that can be injected into SAVE during execution. Plugins accept contents of configuration file and then perform some work.
 */
interface Plugin {
    /**
     * @param testConfig configuration of current test suite
     */
    fun execute(testConfig: TestConfig)
}
