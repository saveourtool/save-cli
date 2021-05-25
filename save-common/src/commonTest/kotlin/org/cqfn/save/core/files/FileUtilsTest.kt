package org.cqfn.save.core.files

import okio.FileSystem
import okio.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

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

    /**
     * directory1
     * |-- file11
     * |-- directory11
     * |   |-- file111
     * |   |-- file112
     * |   |-- directory21
     * |   |   |-- file211
     * |   |   |-- file212
     * |-- directory12
     * |-- directory13
     * |   |-- file131
     * |   |-- directory23
     * |   |   |-- file231
     */
    @Test
    fun `example for findAllFilesMatching`() {
        val directory1 = (tmpDir / "directory1").also(fs::createDirectory)
        val file11 = fs.createFile(directory1 / "file11")
        val directory11 = (directory1 / "directory11").also(fs::createDirectory)
        val file111 = fs.createFile(directory11 / "file111")
        val file112 = fs.createFile(directory11 / "file112")
        val directory21 = (directory11 / "directory21").also(fs::createDirectory)
        val file211 = fs.createFile(directory21 / "file211")
        val file212 = fs.createFile(directory21 / "file212")
        (directory1 / "directory12").also(fs::createDirectory)
        val directory13 = (directory1 / "directory13").also(fs::createDirectory)
        val file131 = fs.createFile(directory13 / "file131")
        val directory23 = (directory13 / "directory23").also(fs::createDirectory)
        val file231 = fs.createFile(directory23 / "file231")

        val result = directory1.findAllFilesMatching { file ->
            file.name.startsWith("file")
        }
            .toList()

        println(result)
        assertContentEquals(
            listOf(listOf(file11), listOf(file111, file112), listOf(file211, file212), listOf(file131), listOf(file231)),
            result
        )
        assertTrue("There should be no empty lists in method's output") { result.none { it.isEmpty() } }
    }
}
