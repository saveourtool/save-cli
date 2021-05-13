package org.cqfn.save.core.plugin

import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.result.TestResult

import okio.Path

/**
 * Plugin that can be injected into SAVE during execution. Plugins accept contents of configuration file and then perform some work.
 */
interface Plugin {
    /**
     * Perform plugin's work.
     *
     * @param saveProperties general configuration of SAVE. todo: is it needed here? Or should [PluginConfig] be passed here?
     * @param testConfig configuration of current test suite
     * @return a sequence of [TestResult]s for each group of test resources
     */
    fun execute(saveProperties: SaveProperties, testConfig: TestConfig): Sequence<TestResult>

    /**
     * Discover groups of resource files which will be used to run tests.
     *
     * @param root root [Path], from where discovering should be started
     * @return a sequence of files, grouped by test
     */
    fun discoverTestFiles(root: Path): Sequence<List<Path>>
}
