package org.cqfn.save.plugins.fix

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.result.DebugInfo
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.core.utils.isCurrentOsWindows

import io.github.petertrr.diffutils.diff
import okio.FileSystem

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Needed tests:
 * - discovering of file pairs
 * - running tool
 */
class FixPluginTest {
    private val fs = FileSystem.SYSTEM
    private val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / FixPluginTest::class.simpleName!!).also {
        fs.createDirectory(it)
    }

    @Test
    fun `should detect two files`() {
        val testFile = fs.createFile(tmpDir / "Test1Test.java")
        val expectedFile = fs.createFile(tmpDir / "Test1Expected.java")

        val pairs = FixPlugin().discoverTestFiles(tmpDir)
            .map { it.first() to it.last() }
            .toList()

        assertEquals(1, pairs.size)
        assertEquals("Test1Expected.java", pairs.single().first.name)
        assertEquals("Test1Test.java", pairs.single().second.name)
    }

    @Test
    fun `should detect two files - among other files`() {
        fs.createFile(tmpDir / "Test2Test.java")
        fs.createFile(tmpDir / "Test2Expected.java")
        fs.createFile(tmpDir / "Something.java")
        fs.createFile(tmpDir / "SomethingExpected.java")
        fs.createFile(tmpDir / "Anything.java")
        fs.createFile(tmpDir / "AnythingTest.java")
        fs.createFile(tmpDir / "CompletelyDifferentTest.java")
        fs.createFile(tmpDir / "NowCompletelyDifferentExpected.java")
        fs.createFile(tmpDir / "AndNowCompletelyDifferent.java")

        val pairs = FixPlugin().discoverTestFiles(tmpDir)
            .map { it.first() to it.last() }
            .toList()

        assertEquals(1, pairs.size)
        assertEquals("Test2Expected.java", pairs.single().first.name)
        assertEquals("Test2Test.java", pairs.single().second.name)
    }

    @Test
    fun `should calculate diff of discovered files in inPlace mode`() {
        val config = fs.createFile(tmpDir / "save.toml")
        val testFile = fs.createFile(tmpDir / "Test3Test.java")
        fs.write(testFile) {
            write("Original file".encodeToByteArray())
        }
        val expectedFile = fs.createFile(tmpDir / "Test3Expected.java")
        fs.write(expectedFile) {
            write("Expected file".encodeToByteArray())
        }

        val diskWithTmpDir = if (isCurrentOsWindows()) "${tmpDir.toString().substringBefore("\\").lowercase()} && " else ""
        val executionCmd = "${diskWithTmpDir}cd $tmpDir && echo Expected file > Test3Test.java"

        val results = FixPlugin(
            TestConfig(config,
            null,
            mutableListOf(FixPluginConfig(executionCmd, inPlace = true))
        )).execute()

        assertEquals(1, results.count(), "Size of results should equal number of pairs")
        assertEquals(TestResult(listOf(expectedFile, testFile), Pass(null), DebugInfo(results.single().debugInfo?.stdout, null, null)), results.single())

        assertTrue("Files should be identical") {
            diff(fs.readLines(testFile), fs.readLines(expectedFile))
                .deltas.isEmpty()
        }
    }

    @Test
    fun `should calculate diff of discovered files with destinationFileSuffix`() {
        val config = fs.createFile(tmpDir / "save.toml")
        val testFile = fs.createFile(tmpDir / "Test3Test.java")
        fs.write(testFile) {
            write("Original file".encodeToByteArray())
        }
        val expectedFile = fs.createFile(tmpDir / "Test3Expected.java")
        fs.write(expectedFile) {
            write("Expected file".encodeToByteArray())
        }
        val diskWithTmpDir = if (isCurrentOsWindows()) "${tmpDir.toString().substringBefore("\\").lowercase()} && " else ""
        val executionCmd = "${diskWithTmpDir}cd $tmpDir && echo Expected file > Test3Test_copy.java"

        val results = FixPlugin(           TestConfig(config,
            null,
            mutableListOf(FixPluginConfig(executionCmd, destinationFileSuffix = "_copy"))
        )).execute()

        assertEquals(1, results.count(), "Size of results should equal number of pairs")
        assertEquals(TestResult(listOf(expectedFile, testFile), Pass(null), DebugInfo(results.single().debugInfo?.stdout, null, null)), results.single())

        assertTrue("Files should be identical") {
            diff(fs.readLines(tmpDir / "Test3Test_copy.java"), fs.readLines(expectedFile))
                .deltas.isEmpty()
        }
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }
}
