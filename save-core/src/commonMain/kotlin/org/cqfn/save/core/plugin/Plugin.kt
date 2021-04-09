package org.cqfn.save.core.plugin

import org.cqfn.save.core.config.TestSuiteConfig

/**
 * Plugin that can be injected into SAVE during execution. Plugins accept contents of configuration file and then perform some work.
 */
interface Plugin {
    /**
     * @param testSuiteConfig configuration of current test suite
     */
    fun execute(testSuiteConfig: TestSuiteConfig)
}
