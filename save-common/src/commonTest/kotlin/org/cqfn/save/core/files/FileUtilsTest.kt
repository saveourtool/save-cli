package org.cqfn.save.core.files

import okio.FileSystem
import okio.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FileUtilsTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var tmpDir: Path

    @BeforeTest
    fun setUp() {
        tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "FileUtilsTest").also(fs::createDirectory)
    }

    @AfterTest
    fun tearDown() {
        if (::tmpDir.isInitialized) {
            fs.deleteRecursively(tmpDir)
        }
    }

    @Test
    fun `example for findDescendantDirectoriesBy`() {
        val directory1 = (tmpDir / "directory1").also(fs::createDirectory)
        fs.createFile(directory1 / "file1")
        val directory11 = (directory1 / "directory11").also(fs::createDirectory)
        fs.createFile(directory11 / "file2")
        fs.createDirectory(directory11 / "directory21")
        val directory12 = (directory1 / "directory12").also(fs::createDirectory)
        val directory13 = (directory1 / "directory13").also(fs::createDirectory)
        val directory23 = (directory13 / "directory23").also(fs::createDirectory)
        fs.createFile(directory23 / "file33")

        val result = directory1.findDescendantDirectoriesBy { dir ->
            fs.list(dir).none { fs.metadata(it).isRegularFile }
        }
            .toList()

        assertEquals(2, result.size)
        assertEquals(directory12, result.first())
        assertEquals(directory13, result[1])
    }
}
