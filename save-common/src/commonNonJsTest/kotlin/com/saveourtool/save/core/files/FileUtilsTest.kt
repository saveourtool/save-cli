package com.saveourtool.save.core.files

import okio.FileSystem
import okio.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
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

        assertContentEquals(
            listOf(listOf(file11), listOf(file111, file112), listOf(file211, file212), listOf(file131), listOf(file231)),
            result
        )
        assertTrue("There should be no empty lists in method's output") { result.none { it.isEmpty() } }
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

    @Test
    fun `create relative path, when config and test resource located in the same directory`() {
        val config = fs.createFile(tmpDir / "save.toml")
        val testFile = fs.createFile(tmpDir / "Test1Test.java")

        // Should be name of current file, since they are located in the same dir
        assertEquals("Test1Test.java", testFile.createRelativePathToTheRoot(config))
    }

    @Test
    fun `create relative path in case of branchy file tree`() {
        fs.createDirectories(tmpDir / "dir2" / "dir3" / "dir4")
        fs.createDirectory(tmpDir / "dir2" / "dir3" / "dir33")

        val config1 = fs.createFile(tmpDir / "save.toml")
        val testFile1 = fs.createFile(tmpDir / "Test1Test.java")

        val config2 = fs.createFile(tmpDir / "dir2" / "save.toml")
        val testFile2 = fs.createFile(tmpDir / "dir2" / "Test2Test.java")

        val config3 = fs.createFile(tmpDir / "dir2" / "dir3" / "save.toml")
        val testFile3 = fs.createFile(tmpDir / "dir2" / "dir3" / "Test3Test.java")

        val testFile33 = fs.createFile(tmpDir / "dir2" / "dir3" / "dir33" / "Test33Test.java")

        val separator = Path.DIRECTORY_SEPARATOR

        assertEquals("Test1Test.java", testFile1.createRelativePathToTheRoot(config1))
        assertEquals("dir2${separator}Test2Test.java", testFile2.createRelativePathToTheRoot(config1))
        assertEquals("dir2${separator}dir3${separator}Test3Test.java", testFile3.createRelativePathToTheRoot(config1))
        assertEquals("dir2${separator}dir3${separator}dir33${separator}Test33Test.java", testFile33.createRelativePathToTheRoot(config1))

        assertEquals("dir33${separator}Test33Test.java", testFile33.createRelativePathToTheRoot(config3))
        assertEquals("dir3${separator}dir33${separator}Test33Test.java", testFile33.createRelativePathToTheRoot(config2))
        val dir4 = tmpDir / "dir2" / "dir3" / "dir4"
        assertEquals("dir2${separator}dir3${separator}dir4", dir4.createRelativePathToTheRoot(config1))
        assertEquals("dir2${separator}dir3${separator}dir4", dir4.createRelativePathToTheRoot(tmpDir))
    }
}
