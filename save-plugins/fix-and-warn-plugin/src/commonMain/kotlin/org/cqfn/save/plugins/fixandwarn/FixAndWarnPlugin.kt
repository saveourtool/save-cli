package org.cqfn.save.plugins.fixandwarn

import okio.FileSystem
import okio.Path
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.config.TestConfigSections
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.plugin.warn.WarnPlugin

import org.cqfn.save.plugins.fix.FixPlugin

class FixAndWarnPlugin(
    testConfig: TestConfig,
    testFiles: List<String>,
    useInternalRedirections: Boolean = true) : Plugin(
    testConfig,
    testFiles,
    useInternalRedirections) {

    private val fixPluginConfig = testConfig.pluginConfigs.filterIsInstance<FixAndWarnPluginConfig>().single().fixPluginConfig

    private val warnPluginConfig = testConfig.pluginConfigs.filterIsInstance<FixAndWarnPluginConfig>().single().warnPluginConfig

    private val fixPlugin = FixPlugin(
        createTestConfigForPlugins(TestConfigSections.FIX),
        testFiles
    )
    private val warnPlugin = WarnPlugin(
        createTestConfigForPlugins(TestConfigSections.WARN),
        testFiles
    )

    private fun createTestConfigForPlugins(type: TestConfigSections): TestConfig {
        return TestConfig(
            testConfig.location,
            testConfig.parentConfig,
            mutableListOf(
                testConfig.pluginConfigs.filterIsInstance<GeneralConfig>().single(),
                if (type == TestConfigSections.FIX) {
                    fixPluginConfig
                } else {
                    warnPluginConfig
                }
            )
        )
    }

    private fun filterFiles(filesSequence: Sequence<List<Path>>, suffix: Regex, match: Boolean): Sequence<List<Path>> {
        val filteredFiles = mutableListOf<List<Path>>()
        filesSequence.forEach { resources ->
            filteredFiles.add(resources.filter { path ->
                if (match) {
                    suffix.matchEntire(path.toString()) != null
                } else {
                    suffix.matchEntire(path.toString()) == null
                }
            })
        }
        return filteredFiles.asSequence()
    }


    override fun handleFiles(files: Sequence<List<Path>>): Sequence<TestResult> {
        testConfig.validateAndSetDefaults()

        // Test files for warn plugin could be obtained by filtering of `fix` resources: excluding `expected` files
        val testFilePattern = warnPluginConfig.resourceNamePattern
        val warnTestFiles = filterFiles(files, testFilePattern, match = true)

        // Warn plugin should process the files, which was fixed by fix plugin and
        // we can get the path of fixed files from warn test files, just applying the same algorithm as in fix plugin
        val testFilesAfterFix = mutableListOf<List<Path>>()
        warnTestFiles.forEach {
            val path = it.single()
            testFilesAfterFix.add(listOf(constructPathForCopyOfTestFile(FixPlugin::class.simpleName!!, path)))
        }

        logDebug("FixPlugin test resources: ${files.toList()}")
        logDebug("WarnPlugin test resources: ${testFilesAfterFix.toList()}")

        val expectedFiles = filterFiles(files, testFilePattern, match = false)

        val originalData: MutableMap<Path, List<String>> = mutableMapOf()
        expectedFiles.forEach { filesList ->
            val filePath = filesList.single()
            val originalFile = fs.readLines(filePath)
            originalData.put(filePath, originalFile)
            val fileWithoutWarnings = originalFile.filter { warnPluginConfig.warningsInputPattern!!.find(it)?.groups == null }
            fs.write(filePath) {
                fileWithoutWarnings.forEach {
                    write((it + "\n").encodeToByteArray())
                }
            }
        }

        val fixTestResults = fixPlugin.handleFiles(files)

        originalData.forEach { (filePath, originalFile) ->
            fs.write(filePath) {
                originalFile.forEach {
                    write((it + "\n").encodeToByteArray())
                }
            }
        }

        // TODO: If we receive just one command for execution, then warn plugin should look at the fix plugin output
        //  for warnings, and not execute command one more time. However, now it works too, but in this case we have
        //  extra actions, which is not good. For the proper work it should be produced refactoring of warn plugin
        //  https://github.com/cqfn/save/issues/164, after which methods of warning comparison will be separated from the common logic
        val warnTestResults = warnPlugin.handleFiles(testFilesAfterFix.asSequence())
        return fixTestResults + warnTestResults
    }

    override fun rawDiscoverTestFiles(resourceDirectories: Sequence<Path>): Sequence<List<Path>> {
        // Test files for fix and warn plugin should be the same, so this will be enough
        return fixPlugin.rawDiscoverTestFiles(resourceDirectories)
    }

    override fun cleanupTempDir() {
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / FixAndWarnPlugin::class.simpleName!!)
        if (fs.exists(tmpDir)) {
            fs.deleteRecursively(tmpDir)
        }
    }
}
