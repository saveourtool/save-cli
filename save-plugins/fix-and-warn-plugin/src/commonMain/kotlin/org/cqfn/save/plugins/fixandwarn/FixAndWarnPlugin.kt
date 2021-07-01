package org.cqfn.save.plugins.fixandwarn

import okio.FileSystem
import okio.Path
import org.cqfn.save.core.config.TestConfig
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
    val fixPlugin = FixPlugin(testConfig, testFiles)
    val warnPlugin = WarnPlugin(testConfig, testFiles)

    override fun handleFiles(files: Sequence<List<Path>>): Sequence<TestResult> {
        TODO("Not yet implemented")
    }

    override fun rawDiscoverTestFiles(resourceDirectories: Sequence<Path>): Sequence<List<Path>> {
        TODO("Not yet implemented")
    }

    override fun cleanupTempDir() {
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / FixAndWarnPlugin::class.simpleName!!)
        if (fs.exists(tmpDir)) {
            fs.deleteRecursively(tmpDir)
        }
    }
}
