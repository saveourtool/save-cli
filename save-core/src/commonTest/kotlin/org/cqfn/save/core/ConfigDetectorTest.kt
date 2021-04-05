package org.cqfn.save.core

import org.cqfn.save.core.files.ConfigDetector

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.use

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConfigDetectorTest {
    // windowsLimitations = true, because SAVE should be cross-platform
    // private val fs = FakeFileSystem(windowsLimitations = true)  // todo: maybe use FakeFileSystem in more complicated tests?
    private val fs = FileSystem.SYSTEM
    private val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / ConfigDetectorTest::class.simpleName!!).also {
        fs.createDirectory(it)
    }
    private val configDetector = ConfigDetector()

    @Test
    fun `should detect single file`() {
        val file = "save.toml".toPath()
        fs.sink(tmpDir / file).close()

        val result = configDetector.configFromFile(file)

        assertNotNull(result)
    }

    @Test
    fun `should detect multiple files`() {
        val parentFile = tmpDir / "save.toml"
        fs.sink(parentFile).close()
        val nestedDir = tmpDir / "nestedDir"
        fs.createDirectory(nestedDir)
        val file = nestedDir / "save.toml"
        fs.sink(file).use { it.flush() }

        assertTrue { fs.metadata(file).isRegularFile }

        val result = configDetector.configFromFile(file)

        assertNotNull(result)
        assertNotNull(result.parentConfig)
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }
}
