package org.cqfn.save.plugins.fixandwarn

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.logging.logDebug
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

    // TODO: get rid of this trick
    private val testFilesSeparator = listOf("SEPARATOR".toPath())

    private val fixPlugin = FixPlugin(testConfig, testFiles)
    private val warnPlugin = WarnPlugin(testConfig, testFiles)

    override fun handleFiles(files: Sequence<List<Path>>): Sequence<TestResult> {
        testConfig.validateAndSetDefaults() // TODO need this? plugins will do this by themselves

        val fixTestFiles = files.takeWhile { it != testFilesSeparator }
        val warnTestFiles = files.dropWhile { it != testFilesSeparator }.drop(1)
        logDebug("WarnPlugin Resourses: ${warnTestFiles.toList()}")
        logDebug("FixPlugin Resourses: ${fixTestFiles.toList()}")

        val fixTestResults = fixPlugin.handleFiles(fixTestFiles)
        println("fixTestResults ${fixTestResults.toList()}")
        val warnTestResults = warnPlugin.handleFiles(warnTestFiles)
        println("warnTestResults ${warnTestResults.toList()}")
        TODO("Not yet implemented")
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
