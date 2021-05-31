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
abstract class Plugin(open val testConfig: TestConfig, private val testFiles: List<String>) {
    /**
     * Perform plugin's work.
     *
     * @return a sequence of [TestResult]s for each group of test resources
     */
    open fun execute(): Sequence<TestResult> = handleFiles(
        // todo: pass individual groups of files to handleFiles? Or it will play bad with batch mode?
        discoverTestFiles(testConfig.directory, null)
    )

    /**
     * Perform plugin's work on a set of files.
     *
     * @param files a sequence of file groups, corresponding to tests.
     * @return a sequence of [TestResult]s for each group of test resources
     */
    abstract fun handleFiles(files: Sequence<List<Path>>): Sequence<TestResult>

    /**
     * Discover groups of resource files which will be used to run tests, applying additional filtering
     * for execution of individual tests.
     *
     * @param root root [Path], from where discovering should be started
     * @param regex default regex for actual warnings in the tool output
     * @return a sequence of files, grouped by test
     */
    fun discoverTestFiles(root: Path, regex: Regex?): Sequence<List<Path>> {
        val rawTestFiles = rawDiscoverTestFiles(root.resourceDirectories(), regex)
        return if (testFiles.isNotEmpty()) {
            rawTestFiles.filter { paths ->
                // test can be specified by the name of one of it's files
                paths.any { it.name in testFiles }
            }
        } else {
            rawTestFiles
        }
    }

    /**
     * Discover groups of resource files which will be used to run tests.
     *
     * @param resourceDirectories a sequence of [Path]s, which contain this plugin's resources
     * @param regex default regex for actual warnings in the tool output
     * @return a sequence of files, grouped by test
     */
    abstract fun rawDiscoverTestFiles(resourceDirectories: Sequence<Path>, regex: Regex?): Sequence<List<Path>>

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
