package com.saveourtool.save.plugin

import com.saveourtool.save.core.config.EvaluatedToolConfig
import com.saveourtool.save.core.config.TestConfig
import com.saveourtool.save.core.files.createFile
import com.saveourtool.save.core.plugin.Plugin
import com.saveourtool.save.core.result.TestResult

import okio.FileSystem
import okio.Path

internal expect val fs: FileSystem

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
    override fun handleFiles(evaluatedToolConfig: EvaluatedToolConfig, files: Sequence<TestFiles>): Sequence<TestResult> = emptySequence()

    override fun rawDiscoverTestFiles(resourceDirectories: Sequence<Path>): Sequence<TestFiles> = emptySequence()

    override fun cleanupTempDir() = Unit
}
