package org.cqfn.save.plugins.fixandwarn

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.config.TestConfigSections
import org.cqfn.save.core.files.createRelativePathToTheRoot
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

    // TODO: get rid of this trick?
    private val testFilesSeparator = listOf("SEPARATOR".toPath())

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
                    testConfig.pluginConfigs.filterIsInstance<FixAndWarnPluginConfig>().single().fixPluginConfig
                } else {
                    testConfig.pluginConfigs.filterIsInstance<FixAndWarnPluginConfig>().single().warnPluginConfig
                }
            )
        )
    }

    override fun handleFiles(files: Sequence<List<Path>>): Sequence<TestResult> {
        testConfig.validateAndSetDefaults()

        val fixTestFiles = files.takeWhile { it != testFilesSeparator }
        val warnTestFiles = files.dropWhile { it != testFilesSeparator }.drop(1)

        // Warn plugin should process the files, which was fixed by fix plugin and
        // since suffix of [fix] test files and [warn] test files should be the same,
        // we can get the path of fixed files from warn test files, just applying the same algorithm as in fix plugin
        val testFilesAfterFix = mutableListOf<List<Path>>()
        warnTestFiles.forEach {
            val path = it.single()
            testFilesAfterFix.add(listOf(constructPathForCopyOfTestFile(FixPlugin::class.simpleName!!, path)))
        }

        logDebug("FixPlugin test resources: ${fixTestFiles.toList()}")
        logDebug("WarnPlugin test resources: ${testFilesAfterFix.toList()}")

        val fixTestResults = fixPlugin.handleFiles(fixTestFiles)
        val warnTestResults = warnPlugin.handleFiles(testFilesAfterFix.asSequence())
        return fixTestResults + warnTestResults
    }

    override fun rawDiscoverTestFiles(resourceDirectories: Sequence<Path>): Sequence<List<Path>> {
        val fixTestFiles = fixPlugin.rawDiscoverTestFiles(resourceDirectories)
        val warnTestFiles = warnPlugin.rawDiscoverTestFiles(resourceDirectories)
        return fixTestFiles + sequenceOf(testFilesSeparator) + warnTestFiles
    }

    override fun cleanupTempDir() {
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / FixAndWarnPlugin::class.simpleName!!)
        if (fs.exists(tmpDir)) {
            fs.deleteRecursively(tmpDir)
        }
    }
}
