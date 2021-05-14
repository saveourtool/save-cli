package org.cqfn.save.core.plugin

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.result.TestResult

/**
 * Plugin that can be injected into SAVE during execution. Plugins accept contents of configuration file and then perform some work.
 */
interface Plugin {
    /**
     * Perform plugin's work.
     *
     * @param testConfig configuration of current test suite
     * @return a sequence of [TestResult]s for each group of test resources
     */
    fun execute(testConfig: TestConfig): Sequence<TestResult>
}
