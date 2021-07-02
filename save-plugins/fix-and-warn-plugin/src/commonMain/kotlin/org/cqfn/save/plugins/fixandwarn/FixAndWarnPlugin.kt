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
        testConfig.validateAndSetDefaults() // TODO need this? plugins will do this by themselves

        val fixTestFiles = files.takeWhile { it != testFilesSeparator }

        /*
        val warnTestFilesTemp = mutableListOf<List<Path>>()
        val expectedFilesPattern = testConfig.pluginConfigs.filterIsInstance<FixAndWarnPluginConfig>()
            .single().fixPluginConfig.resourceNameExpected

        fixTestFiles.forEach { testFiles ->
            warnTestFilesTemp.add(testFiles.filter {
                !it.toString().contains(expectedFilesPattern)
            })
        }
         */
        val warnTestFiles = files.dropWhile { it != testFilesSeparator }.drop(1)
        val warnTestFilesTemp = mutableListOf<List<Path>>()
        warnTestFiles.forEach {
            val path = it.single()
            val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / FixPlugin::class.simpleName!!)
            val relativePath = path.createRelativePathToTheRoot(testConfig.getRootConfig().location)
            warnTestFilesTemp.add(listOf(tmpDir / relativePath / path.name))
        }
        println("warnTestFilesTemp ${warnTestFilesTemp}")
        logDebug("WarnPlugin test resources: ${warnTestFiles.toList()}")
        logDebug("FixPlugin test resources: ${fixTestFiles.toList()}")

        val fixTestResults = fixPlugin.handleFiles(fixTestFiles)
        // TODO Warn actually need to look at the fix plugin results
        val warnTestResults = warnPlugin.handleFiles(warnTestFilesTemp.asSequence())
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
