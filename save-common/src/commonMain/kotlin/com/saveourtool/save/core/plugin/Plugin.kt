package com.saveourtool.save.core.plugin

import com.saveourtool.save.core.config.*
import com.saveourtool.save.core.files.createRelativePathToTheRoot
import com.saveourtool.save.core.files.findDescendantDirectoriesBy
import com.saveourtool.save.core.files.parentsWithSelf
import com.saveourtool.save.core.logging.logDebug
import com.saveourtool.save.core.logging.logTrace
import com.saveourtool.save.core.result.Ignored
import com.saveourtool.save.core.result.TestResult
import com.saveourtool.save.core.utils.PathSerializer
import com.saveourtool.save.core.utils.ProcessBuilder

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

import kotlinx.serialization.Serializable

/**
 * Plugin that can be injected into SAVE during execution. Plugins accept contents of configuration file and then perform some work.
 *
 * @property testConfig
 * @property testFiles a list of files (test resources or save.toml configs)
 * @property fs describes the current file system
 * @property useInternalRedirections whether to redirect stdout/stderr for internal purposes
 * @property redirectTo a file where process output and errors should be redirected. If null, output will be returned as [ExecutionResult.stdout] and [ExecutionResult.stderr].
 */
@Suppress("TooManyFunctions")
abstract class Plugin(
    val testConfig: TestConfig,
    protected val testFiles: List<String>,
    protected val fs: FileSystem,
    private val useInternalRedirections: Boolean,
    protected val redirectTo: Path?,
) {
    /**
     * Instance that is capable of executing processes
     */
    val pb = ProcessBuilder(useInternalRedirections, fs)

    /**
     * Perform plugin's work.
     *
     * @param evaluatedToolConfig a configuration for evaluated tool
     * @return a sequence of [TestResult]s for each group of test resources
     */
    fun execute(evaluatedToolConfig: EvaluatedToolConfig): Sequence<TestResult> {
        clean()
        val testFilesList = discoverTestFiles(testConfig.directory).toList()

        val excludedTests = testConfig
            .pluginConfigs
            .filterIsInstance<GeneralConfig>()
            .singleOrNull()
            ?.excludedTests

        if (!excludedTests.isNullOrEmpty()) {
            logDebug("Excluded tests for [${testConfig.location}] : $excludedTests")
        }
        clean()
        return if (testFilesList.isNotEmpty()) {
            // fixme: remove this logging and convert `testFilesList` back to Sequence
            // or at least make `logDebug` accept lazy messages
            logTrace("Discovered the following test resources: $testFilesList")
            val (excludedTestFiles, actualTestFiles) = testFilesList.partition {
                isExcludedTest(it, excludedTests)
            }
            val excludedTestResults = excludedTestFiles.map {
                TestResult(it, Ignored("Excluded by configuration"))
            }
            handleFiles(evaluatedToolConfig, actualTestFiles.asSequence()) + excludedTestResults
        } else {
            emptySequence()
        }
    }

    /**
     * Perform plugin's work on a set of files.
     *
     * @param evaluatedToolConfig a configuration for evaluated tool
     * @param files a sequence of file groups, corresponding to tests.
     * @return a sequence of [TestResult]s for each group of test resources
     */
    abstract fun handleFiles(evaluatedToolConfig: EvaluatedToolConfig, files: Sequence<TestFiles>): Sequence<TestResult>

    /**
     * Discover groups of resource files which will be used to run tests, applying additional filtering
     * for execution of individual tests.
     *
     * @param root root [Path], from where discovering should be started
     * @return a sequence of files, grouped by test
     */
    @Suppress(
        "TOO_LONG_FUNCTION",
        "TOO_MANY_LINES_IN_LAMBDA",
        "AVOID_NULL_CHECKS",
        "SwallowedException",
    )
    fun discoverTestFiles(root: Path): Sequence<TestFiles> {
        val rawTestFiles = rawDiscoverTestFiles(root.resourceDirectories())
            .filterNot { fs.metadata(it.test).isDirectory }

        // fixme: move this filtering on higher level under https://github.com/saveourtool/save/issues/336
        val existingTestFiles = testFiles.map { file ->
            file.takeIf { fs.exists(it.toPath()) }
        }
        return if (testFiles.isNotEmpty()) {
            val foundTests = rawTestFiles.filter { rawTestFile ->
                existingTestFiles.any { testFile ->
                    if (testFile != null) {
                        (rawTestFile.test.parentsWithSelf()).any { rawTestFileDir ->
                            testFile == rawTestFileDir.toString()
                        }
                    } else {
                        logDebug("Could not find the next test or directory: $testFile, check the path is correct.")
                        false
                    }
                }
            }.toList()
            val foundTestsInDir = foundTests.filter { testFile -> testFile.test.toString() in testFiles }
            if (foundTests.isNotEmpty()) {
                logDebug("Following tests have been detected for the current plugin and directory: " +
                        "${foundTestsInDir.map { it.test.toString() } }. ")
            }
            return foundTests.asSequence()
        } else {
            rawTestFiles
        }
    }

    private fun isExcludedTest(testFiles: TestFiles, excludedTests: List<String>?): Boolean {
        // common root of the test repository (not a location of a current test)
        val testRepositoryRoot = testConfig.getRootConfig().location
        // creating relative to root path from a test file
        // "Expected" file for Fix plugin
        val testFileRelative =
                (testFiles.test.createRelativePathToTheRoot(testRepositoryRoot))
                    .replace('\\', '/')

        // excluding tests that are included in the excluded list
        return excludedTests
            ?.map { it.replace('\\', '/') }
            ?.contains(testFileRelative)
            ?: false
    }

    /**
     * Discover groups of resource files which will be used to run tests.
     *
     * @param resourceDirectories a sequence of [Path]s, which contain this plugin's resources
     * @return a sequence of files, grouped by test
     */
    abstract fun rawDiscoverTestFiles(resourceDirectories: Sequence<Path>): Sequence<TestFiles>

    /**
     * Method for cleaning up a directory.
     */
    abstract fun cleanupTempDir()

    /**
     * Method for call cleanupTempDir().
     *
     * @throws TempDirException when deleting temp dir
     */
    @Suppress(
        "TooGenericExceptionCaught",
        "SwallowedException"
    )
    private fun clean() {
        try {
            cleanupTempDir()
        } catch (e: Exception) {
            throw TempDirException("Could not delete temp dir, cause: ${e.message}")
        }
    }

    /**
     * Method for creating temp dir.
     *
     * @param tmpDir
     * @throws TempDirException when creating temp dir
     */
    @Suppress(
        "TooGenericExceptionCaught",
        "SwallowedException"
    )
    protected fun createTempDir(tmpDir: Path) {
        try {
            if (!fs.exists(tmpDir)) {
                fs.createDirectories(tmpDir)
            }
        } catch (e: Exception) {
            throw TempDirException("Could not create temp dir, cause: ${e.message}")
        }
    }

    /**
     *  Construct path for copy of test file, over which the plugins will be working on
     *
     *  @param dirName name of the tmp subdirectory
     *  @param path original path of test file
     *  @return path for copy of test file
     */
    protected fun constructPathForCopyOfTestFile(dirName: String, path: Path): Path {
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / dirName)
        val relativePath = path.createRelativePathToTheRoot(testConfig.getRootConfig().location)
        return tmpDir / relativePath
    }

    /**
     * Returns a sequence of directories, where resources for this plugin may be located.
     * This takes into account, that if underlying directory contains its own SAVE config,
     * then this plugin shouldn't touch these resources; it should be done by plugins from that config.
     *
     * @return a sequence of directories possibly containing this plugin's test resources
     */
    private fun Path.resourceDirectories(): Sequence<Path> = findDescendantDirectoriesBy(true) { file ->
        // this matches directories which contain their own SAVE config
        fs.metadata(file).isDirectory && fs.list(file).none { it.isSaveTomlConfig() }
    }

    /**
     * Represents resources for a particular test handled by a plugin
     */
    @Suppress("USE_DATA_CLASS")
    interface TestFiles {
        /**
         * path to test file
         */
        val test: Path

        /**
         * @param root path to calculate relative paths
         * @return copy of `this` with relativized paths
         */
        fun withRelativePaths(root: Path): TestFiles
    }

    /**
     * @property test test file
     */
    @Serializable
    data class Test(@Serializable(with = PathSerializer::class) override val test: Path) : TestFiles {
        override fun withRelativePaths(root: Path) =
                copy(test = test.createRelativePathToTheRoot(root).toPath())
    }
}
