package org.cqfn.save.reporter.json

import org.cqfn.save.core.files.readFile
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.result.Crash
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.plugin.MockPlugin
import org.cqfn.save.reporter.Report

import okio.FileSystem
import okio.buffer

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.decodeFromString

class JsonReporterTest {
    private val fs = FileSystem.SYSTEM
    private val tmpDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / JsonReporterTest::class.simpleName!!

    @BeforeTest
    fun setUp() {
        if (fs.exists(tmpDir)) {
            fs.deleteRecursively(tmpDir)
        }
        fs.createDirectory(tmpDir)
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }

    @Test
    fun `should produce valid serialized Report from ordinary data`() {
        val tmpFile = tmpDir / "test1"
        val jsonReporter = JsonReporter(fs.sink(tmpFile).buffer())
        jsonReporter.beforeAll()
        jsonReporter.onSuiteStart("suite1")
        jsonReporter.onSuiteEnd("suite1")
        jsonReporter.afterAll()
        jsonReporter.out.close()

        val reports: List<Report> = jsonReporter.json.decodeFromString(fs.readFile(tmpFile))
        assertEquals(1, reports.size)
    }

    @Test
    fun `should produce valid serialized Report when plugin crashes`() {
        val tmpFile = tmpDir / "test2"
        val jsonReporter = JsonReporter(fs.sink(tmpFile).buffer())
        val mockPlugin = MockPlugin(tmpFile.parent!!)
        jsonReporter.beforeAll()
        jsonReporter.onSuiteStart("suite1")
        jsonReporter.onPluginInitialization(mockPlugin)
        jsonReporter.onPluginExecutionStart(mockPlugin)
        jsonReporter.onEvent(
            TestResult(
                Plugin.Test(tmpFile),
                Crash("IllegalArgumentException", "foo")
            )
        )
        jsonReporter.onPluginExecutionEnd(mockPlugin)
        jsonReporter.onSuiteEnd("suite1")
        jsonReporter.afterAll()
        jsonReporter.out.close()

        val serializedReports = fs.readFile(tmpFile)
        println("Serialized representation of reports: $serializedReports")
        val reports: List<Report> = jsonReporter.json.decodeFromString(serializedReports)
        assertEquals(1, reports.size)
    }
}
