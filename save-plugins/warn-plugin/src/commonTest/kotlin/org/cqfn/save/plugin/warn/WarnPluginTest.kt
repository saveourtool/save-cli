package org.cqfn.save.plugin.warn

import org.cqfn.save.core.config.LanguageType
import org.cqfn.save.core.config.ReportType
import org.cqfn.save.core.config.ResultOutputType
import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.files.readLines

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
class WarnPluginTest {
    private val fs = FileSystem.SYSTEM
    private val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / WarnPluginTest::class.simpleName!!).also {
        fs.createDirectory(it)
    }

    @Test
    fun `should detect two files`() {
        val testFile = fs.createFile(tmpDir / "Test1Test.java")
        val expectedFile = fs.createFile(tmpDir / "Test1Expected.java")

//        val pairs = FixPlugin().discoverFilePairs(listOf(testFile, expectedFile))
//        assertEquals(1, pairs.size)
//        assertEquals("Test1Expected.java", pairs.single().first.name)
//        assertEquals("Test1Test.java", pairs.single().second.name)
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }
}
