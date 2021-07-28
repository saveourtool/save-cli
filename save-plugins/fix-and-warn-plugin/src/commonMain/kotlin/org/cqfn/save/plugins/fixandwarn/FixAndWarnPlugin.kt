package org.cqfn.save.plugins.fixandwarn

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.plugin.PluginConfig
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.plugin.warn.WarnPlugin
import org.cqfn.save.plugin.warn.WarnPluginConfig
import org.cqfn.save.plugins.fix.FixPlugin
import org.cqfn.save.plugins.fix.FixPluginConfig

import okio.FileSystem
import okio.Path

private typealias WarningsList = MutableList<Pair<Int, String>>

/**
 * A plugin that runs an executable on a file, and combines two actions: fix and warn
 * Plugin fixes test file, warns if something couldn't be auto-corrected after fix
 * and compares output with expected output during one execution.
 */
class FixAndWarnPlugin(
    testConfig: TestConfig,
    testFiles: List<String>,
    fileSystem: FileSystem,
    useInternalRedirections: Boolean = true) : Plugin(
    testConfig,
    testFiles,
    fileSystem,
    useInternalRedirections) {
    private lateinit var fixPluginConfig: FixPluginConfig
    private lateinit var warnPluginConfig: WarnPluginConfig
    private lateinit var fixPlugin: FixPlugin
    private lateinit var warnPlugin: WarnPlugin

    private fun initOrUpdateConfigs() {
        fixPluginConfig = testConfig.pluginConfigs.filterIsInstance<FixAndWarnPluginConfig>().single().fix
        warnPluginConfig = testConfig.pluginConfigs.filterIsInstance<FixAndWarnPluginConfig>().single().warn
        fixPlugin = FixPlugin(createTestConfigForPlugins(fixPluginConfig), testFiles, fs)
        warnPlugin = WarnPlugin(createTestConfigForPlugins(warnPluginConfig), testFiles, fs)
    }

    /**
     * Create TestConfig same as current, but with corresponding plugin configs list for nested [fix] and [warn] sections
     *
     * @param pluginConfig [fix] or [warn] config of nested section
     * @return TestConfig for corresponding section
     */
    private fun createTestConfigForPlugins(pluginConfig: PluginConfig) = TestConfig(
        testConfig.location,
        testConfig.parentConfig,
        mutableListOf(
            testConfig.pluginConfigs.filterIsInstance<GeneralConfig>().single(),
            pluginConfig
        ),
        fs,
    )

    override fun handleFiles(files: Sequence<List<Path>>): Sequence<TestResult> {
        testConfig.validateAndSetDefaults()
        // Need to update private fields after validation
        initOrUpdateConfigs()
        val testFilePattern = warnPluginConfig.resourceNamePattern
        val expectedFiles = files.filterTestResources(testFilePattern, match = false)

        // Remove (in place) warnings from test files before fix plugin execution
        val filesAndTheirWarningsMap = removeWarningsFromExpectedFiles(expectedFiles)

        val fixTestResults = fixPlugin.handleFiles(files).toList()

        val (fixTestResultsPassed, fixTestResultsFailed) = fixTestResults.partition { it.status is Pass }

        val expectedFilesWithPass = expectedFiles.filter { expectedFile ->
            fixTestResultsPassed.map { it.resources.toList()[0] }.contains(expectedFile)
        }

        // Fill back original data with warnings
        filesAndTheirWarningsMap.forEach { (filePath, warningsList) ->
            val fileData = fs.readLines(filePath) as MutableList
            // Append warnings into appropriate place
            warningsList.forEach { (line, warningMsg) ->
                fileData.add(line, warningMsg)
            }
            fs.write(filePath) {
                fileData.forEach {
                    write((it + "\n").encodeToByteArray())
                }
            }
        }

        // TODO: If we receive just one command for execution, and want to avoid extra executions
        // TODO: then warn plugin should look at the fix plugin output for actual warnings, and not execute command one more time.
        // TODO: However it's required changes in warn plugin logic (it's should be able to compare expected and actual warnings from different places),
        // TODO: this probably could be obtained after https://github.com/cqfn/save/issues/164,
        val warnTestResults = warnPlugin.handleFiles(expectedFilesWithPass.map { listOf(it) })
        return fixTestResultsFailed.asSequence() + warnTestResults
    }

    override fun rawDiscoverTestFiles(resourceDirectories: Sequence<Path>): Sequence<List<Path>> {
        initOrUpdateConfigs()
        // Test files for fix and warn plugin should be the same, so this will be enough
        return fixPlugin.rawDiscoverTestFiles(resourceDirectories)
    }

    /**
     * Filter test resources
     *
     * @param suffix regex pattern of test resource
     * @param match whether to keep elements which matches current pattern or to keep elements, which is not
     * @return filtered list of files
     */
    private fun Sequence<List<Path>>.filterTestResources(suffix: Regex, match: Boolean) = map { resources ->
        resources.single { path ->
            if (match) {
                suffix.matchEntire(path.toString()) != null
            } else {
                suffix.matchEntire(path.toString()) == null
            }
        }
    }

    /**
     * Remove warnings from the given files, which satisfy pattern from [warn] plugin and save data about warnings, which were deleted
     *
     * @files files to be modified
     *
     * @return map of files and theirs list of warnings
     */
    private fun removeWarningsFromExpectedFiles(files: Sequence<Path>): MutableMap<Path, WarningsList> {
        val filesAndTheirWarningsMap: MutableMap<Path, WarningsList> = mutableMapOf()
        files.forEach { file ->
            val fileData = fs.readLines(file)
            filesAndTheirWarningsMap[file] = mutableListOf()

            val fileDataWithoutWarnings = fileData.filterIndexed { index, line ->
                val isLineWithWarning = (warnPluginConfig.warningsInputPattern!!.find(line)?.groups != null)
                if (isLineWithWarning) {
                    filesAndTheirWarningsMap[file]!!.add(index to line)
                }
                !isLineWithWarning
            }
            writeDataWithoutWarnings(file, filesAndTheirWarningsMap, fileDataWithoutWarnings)
        }
        return filesAndTheirWarningsMap
    }

    private fun writeDataWithoutWarnings(
        file: Path,
        filesAndTheirWarningsMap: MutableMap<Path, WarningsList>,
        fileDataWithoutWarnings: List<String>
    ) {
        if (filesAndTheirWarningsMap[file]!!.isEmpty()) {
            filesAndTheirWarningsMap.remove(file)
        } else {
            fs.write(file) {
                fileDataWithoutWarnings.forEach {
                    write((it + "\n").encodeToByteArray())
                }
            }
        }
    }

    override fun cleanupTempDir() {
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / FixAndWarnPlugin::class.simpleName!!)
        if (fs.exists(tmpDir)) {
            fs.deleteRecursively(tmpDir)
        }
    }
}
