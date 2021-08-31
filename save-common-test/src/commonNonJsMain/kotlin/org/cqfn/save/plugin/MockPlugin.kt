package org.cqfn.save.plugin

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.result.TestResult

import okio.FileSystem
import okio.Path

private val fs: FileSystem = FileSystem.SYSTEM

/**
 * No-op implementation of [Plugin] that can be used to test reporters, which expect only a class name of the plugin.
 */
class MockPlugin(baseDir: Path, testFiles: List<String> = emptyList()) : Plugin(
    TestConfig((baseDir / "save.toml").also { fs.createFile(it) }, null, fs = fs),
    testFiles,
    fs,
    useInternalRedirections = true,
    redirectTo = null
) {
    override fun handleFiles(files: Sequence<List<Path>>): Sequence<TestResult> = emptySequence()

    override fun rawDiscoverTestFiles(resourceDirectories: Sequence<Path>): Sequence<List<Path>> = emptySequence()

    override fun cleanupTempDir() = Unit
}
