package org.cqfn.save.core

import org.cqfn.save.core.files.ConfigDetector
import org.cqfn.save.core.files.createFile

import okio.FileSystem

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
        val file = fs.createFile(tmpDir / "save.toml")

        val result = configDetector.configFromFile(file)

        assertNotNull(result)
    }

    @Test
    fun `should fail on the invalid file`() {
        val file = fs.createFile(tmpDir / "random.text")
        assertFailsWith<IllegalArgumentException> {
            configDetector.configFromFile(file)
        }
    }

    @Test
    fun `should detect single file from a directory`() {
        fs.createFile(tmpDir / "save.toml")

        val result = configDetector.configFromFile(tmpDir)

        assertNotNull(result)
    }

    @Test
    fun `should detect starting from bottom`() {
        fs.createFile(tmpDir / "save.toml")
        val nestedDir = tmpDir / "nestedDir"
        fs.createDirectory(nestedDir)
        val file = fs.createFile(nestedDir / "save.toml")

        val result = configDetector.configFromFile(file)

        assertNotNull(result)
        assertNotNull(result.parentConfig)
        assertTrue(result.parentConfig!!.childConfigs.isNotEmpty())
    }

    @Test
    fun `should detect starting from bottom with multiple parent configs`() {
        fs.createFile(tmpDir / "save.toml")
        val nestedDir1 = tmpDir / "nestedDir1"
        fs.createDirectory(nestedDir1)
        fs.createFile(nestedDir1 / "save.toml")
        val nestedDir2 = nestedDir1 / "nestedDir2"
        fs.createDirectory(nestedDir2)
        val file = fs.createFile(nestedDir2 / "save.toml")

        val result = configDetector.configFromFile(file)

        assertNotNull(result)
        assertEquals(2, result.parentConfigs().count())
        result.parentConfigs().forEach {
            assertTrue(it.childConfigs.isNotEmpty(), "Child configs for config @${it.location} are empty")
        }
    }

    @Test
    fun `should detect multiple files starting from top`() {
        val parentFile = fs.createFile(tmpDir / "save.toml")
        val nestedDir = tmpDir / "nestedDir"
        fs.createDirectory(nestedDir)
        fs.createFile(nestedDir / "save.toml")

        val result = configDetector.configFromFile(parentFile)

        assertNotNull(result)
        assertTrue(result.childConfigs.isNotEmpty())
    }

    @Test
    fun `should detect multiple files starting from the middle`() {
        // save.toml
        // nestedDir1
        // |___save.toml  <-- starting search from this
        // |___nestedDir2
        // | |___save.toml
        // |___nestedDir3
        // | |___nestedDir4
        // | | |___save.toml
        fs.createFile(tmpDir / "save.toml")
        val nestedDir1 = tmpDir / "nestedDir1"
        fs.createDirectory(nestedDir1)
        val file = fs.createFile(nestedDir1 / "save.toml")
        fs.createDirectory(nestedDir1 / "nestedDir2")
        fs.createDirectory(nestedDir1 / "nestedDir3")
        fs.createFile(nestedDir1 / "nestedDir2" / "save.toml")
        fs.createDirectory(nestedDir1 / "nestedDir3" / "nestedDir4")
        fs.createFile(nestedDir1 / "nestedDir3" / "nestedDir4" / "save.toml")

        val result = configDetector.configFromFile(file)

        assertNotNull(result)
        assertEquals(1, result.parentConfigs().count())
        assertEquals(2, result.childConfigs.size)
    }

    @Test
    fun `should detect config file from single Test file`() {
        val file = fs.createFile(tmpDir / "Feature1Test.java")
        fs.createFile(tmpDir / "save.toml")

        val result = configDetector.configFromFile(file)

        assertNotNull(result)
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }
}
