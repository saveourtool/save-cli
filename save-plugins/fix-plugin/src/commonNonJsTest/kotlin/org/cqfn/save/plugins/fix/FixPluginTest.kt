package org.cqfn.save.plugins.fix

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.result.DebugInfo
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.core.utils.ProcessBuilder
import org.cqfn.save.core.utils.isCurrentOsWindows

import io.github.petertrr.diffutils.diff
import okio.FileSystem

import kotlin.random.Random
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
    private val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "${FixPluginTest::class.simpleName!!}-${Random.nextInt()}").also {
        fs.createDirectory(it)
    }

    @Test
    fun `should detect two files`() {
        fs.createFile(tmpDir / "save.toml")
        fs.createFile(tmpDir / "Test1Test.java")
        fs.createFile(tmpDir / "Test1Expected.java")

        val pairs = discoverFilePairs()

        assertEquals(1, pairs.size)
        assertEquals("Test1Test.java", pairs.single().first.name)
        assertEquals("Test1Expected.java", pairs.single().second.name)
    }
    @Test
    fun `should detect two files - among other files`() {
        fs.createFile(tmpDir / "save.toml")
        fs.createFile(tmpDir / "Test2Test.java")
        fs.createFile(tmpDir / "Test2Expected.java")
        fs.createFile(tmpDir / "Something.java")
        fs.createFile(tmpDir / "SomethingExpected.java")
        fs.createFile(tmpDir / "Anything.java")
        fs.createFile(tmpDir / "AnythingTest.java")
        fs.createFile(tmpDir / "CompletelyDifferentTest.java")
        fs.createFile(tmpDir / "NowCompletelyDifferentExpected.java")
        fs.createFile(tmpDir / "AndNowCompletelyDifferent.java")

        val pairs = discoverFilePairs()

        assertEquals(1, pairs.size)
        assertEquals("Test2Test.java", pairs.single().first.name)
        assertEquals("Test2Expected.java", pairs.single().second.name)
    }

    @Test
    fun `should calculate diff of discovered files`() {
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
        val executionCmd = "${diskWithTmpDir}cd $tmpDir && echo Expected file >"

        val results = FixPlugin(TestConfig(config,
            null,
            mutableListOf(
                FixPluginConfig(executionCmd),
                GeneralConfig("", listOf(""), "", "")
            ), fs),
            testFiles = emptyList(),
            fs,
            useInternalRedirections = false
        ).execute()

        assertEquals(1, results.count(), "Size of results should equal number of pairs")
        assertEquals(TestResult(FixPlugin.FixTestFiles(testFile, expectedFile), Pass(null),
            DebugInfo(results.single().debugInfo?.execCmd, results.single().debugInfo?.stdout, results.single().debugInfo?.stderr, null)), results.single())
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / FixPlugin::class.simpleName!!)
        assertTrue("Files should be identical") {
            diff(fs.readLines(tmpDir / "Test3Test.java"), fs.readLines(expectedFile))
                .deltas.isEmpty()
        }
    }

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun `test for batchSize`() {
        val config = fs.createFile(tmpDir / "save.toml")
        val testFile1 = fs.createFile(tmpDir / "Test3Test.java")
        val testFile2 = fs.createFile(tmpDir / "Test4Test.java")
        fs.write(testFile1) {
            write("Original file".encodeToByteArray())
        }
        fs.write(testFile2) {
            write("Original file".encodeToByteArray())
        }
        val expectedFile1 = fs.createFile(tmpDir / "Test3Expected.java")
        val expectedFile2 = fs.createFile(tmpDir / "Test4Expected.java")
        fs.write(expectedFile1) {
            write("Expected file".encodeToByteArray())
        }
        fs.write(expectedFile2) {
            write("Expected file".encodeToByteArray())
        }
        val diskWithTmpDir = if (isCurrentOsWindows()) "${tmpDir.toString().substringBefore("\\").lowercase()} && " else ""
        // FixMe: after https://github.com/cqfn/save/issues/158
        val executionCmd = if (isCurrentOsWindows()) {
            // We call ProcessBuilder ourselves, because the command ">" does not work for the list of files
            ProcessBuilder(false, fs).exec("echo Expected file > $testFile2", "", null)
            "${diskWithTmpDir}cd $tmpDir && echo Expected file >"
        } else {
            "${diskWithTmpDir}cd $tmpDir && echo Expected file | tee"
        }

        val fixPluginConfig = if (isCurrentOsWindows()) FixPluginConfig(executionCmd, 2) else FixPluginConfig(executionCmd, 2, " ")

        val results = FixPlugin(TestConfig(config,
            null,
            mutableListOf(
                fixPluginConfig,
                GeneralConfig("", listOf(""), "", "")
            ), fs),
            testFiles = emptyList(),
            fs,
            useInternalRedirections = false
        ).execute()

        // We call ProcessBuilder ourselves, because the command ">" does not work for the list of files
        ProcessBuilder(false, fs).exec("echo Expected file > $testFile2", "", null)

        assertEquals(2, results.count(), "Size of results should equal number of pairs")
        assertTrue(results.all {
            it.status == Pass(null)
        })
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / FixPlugin::class.simpleName!!)
        assertTrue("Files should be identical") {
            diff(fs.readLines(tmpDir / "Test3Test.java"), fs.readLines(expectedFile1))
                .deltas.isEmpty()
        }
        assertTrue("Files should be identical") {
            diff(fs.readLines(tmpDir / "Test4Test.java"), fs.readLines(expectedFile2))
                .deltas.isEmpty()
        }
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }

    internal fun discoverFilePairs() = FixPlugin(
        TestConfig(tmpDir / "save.toml", null, mutableListOf(FixPluginConfig("")), fs),
        testFiles = emptyList(),
        fs,
        useInternalRedirections = false
    )
        .discoverTestFiles(tmpDir)
        .map { it as FixPlugin.FixTestFiles }
        .map { it.test to it.expected }
        .toList()
}
