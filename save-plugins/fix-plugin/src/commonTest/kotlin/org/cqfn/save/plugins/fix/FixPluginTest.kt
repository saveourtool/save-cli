package org.cqfn.save.plugins.fix

import org.cqfn.save.core.config.LanguageType
import org.cqfn.save.core.config.ReportType
import org.cqfn.save.core.config.ResultOutputType
import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.files.readLines

import io.github.petertrr.diffutils.diff
import okio.FileSystem

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val mockConfig = SaveProperties(
    testConfig = ".",
    parallelMode = false,
    threads = 1,
    propertiesFile = ".",
    debug = true,
    quiet = false,
    reportType = ReportType.PLAIN,
    baseline = null,
    excludeSuites = null,
    includeSuites = null,
    language = LanguageType.KOTLIN,
    testRootPath = ".",
    resultOutput = ResultOutputType.STDOUT,
    configInheritance = true,
    ignoreSaveComments = true,
    reportDir = "."
)

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

        val pairs = FixPlugin().discoverFilePairs(listOf(testFile, expectedFile))

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

        val pairs = FixPlugin().discoverFilePairs(fs.list(tmpDir))

        assertEquals(1, pairs.size)
        assertEquals("Test2Expected.java", pairs.single().first.name)
        assertEquals("Test2Test.java", pairs.single().second.name)
    }

    @Test
    fun `should calculate diff of discovered files in inPlace mode`() {
        val testFile = fs.createFile(tmpDir / "Test3Test.java")
        fs.write(testFile) {
            write("Original file".encodeToByteArray())
        }
        val expectedFile = fs.createFile(tmpDir / "Test3Expected.java")
        fs.write(expectedFile) {
            write("Expected file".encodeToByteArray())
        }

        FixPlugin().execute(
            mockConfig,
            TestConfig(tmpDir,
                null,
                listOf(FixPluginConfig("cd $tmpDir && echo Expected file> Test3Test.java", inPlace = true, testResources = listOf(testFile, expectedFile)))
            )
        )

        assertTrue("Files should be identical") {
            diff(fs.readLines(testFile), fs.readLines(expectedFile))
                .deltas.isEmpty()
        }
    }

    @Test
    fun `should calculate diff of discovered files with destinationFileSuffix`() {
        val testFile = fs.createFile(tmpDir / "Test3Test.java")
        fs.write(testFile) {
            write("Original file".encodeToByteArray())
        }
        val expectedFile = fs.createFile(tmpDir / "Test3Expected.java")
        fs.write(expectedFile) {
            write("Expected file".encodeToByteArray())
        }

        FixPlugin().execute(
            mockConfig,
            TestConfig(tmpDir,
                null,
                listOf(FixPluginConfig("cd $tmpDir && echo Expected file> Test3Test_copy.java", destinationFileSuffix = "_copy", testResources = listOf(testFile, expectedFile)))
            )
        )

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
