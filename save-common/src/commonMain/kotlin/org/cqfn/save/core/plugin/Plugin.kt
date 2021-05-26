package org.cqfn.save.core.plugin

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.config.isSaveTomlConfig
import org.cqfn.save.core.files.findDescendantDirectoriesBy
import org.cqfn.save.core.result.TestResult

import okio.FileSystem
import okio.Path

/**
 * Plugin that can be injected into SAVE during execution. Plugins accept contents of configuration file and then perform some work.
 * @property testConfig
 */
abstract class Plugin(open val testConfig: TestConfig) {
    /**
     * Perform plugin's work.
     *
     * @return a sequence of [TestResult]s for each group of test resources
     */
    abstract fun execute(): Sequence<TestResult>

    /**
     * Discover groups of resource files which will be used to run tests.
     *
     * @param root root [Path], from where discovering should be started
     * @return a sequence of files, grouped by test
     */
    abstract fun discoverTestFiles(root: Path): Sequence<List<Path>>

    /**
     * Returns a sequence of directories, where resources for this plugin may be located.
     * This takes into account, that if underlying directory contains it's own SAVE config,
     * then this plugin shouldn't touch these resources; it should be done by plugins from that config.
     *
     * @param fs a [FileSystem] which is used to traverse the directory hierarchy
     * @return a sequence of directories possibly containing this plugin's test resources
     */
    fun Path.resourceDirectories(fs: FileSystem = FileSystem.SYSTEM): Sequence<Path> = findDescendantDirectoriesBy(true) { file ->
        // this matches directories which contain their own SAVE config
        fs.metadata(file).isRegularFile || fs.list(file).none { it.isSaveTomlConfig() }
    }
}
