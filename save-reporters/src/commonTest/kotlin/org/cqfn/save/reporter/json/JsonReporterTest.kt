package org.cqfn.save.reporter.json

import org.cqfn.save.core.files.readFile
import org.cqfn.save.reporter.Report

import okio.ExperimentalFileSystem
import okio.FileSystem
import okio.buffer

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalFileSystem::class)
class JsonReporterTest {
    private val fs = FileSystem.SYSTEM

    @BeforeTest
    fun setUp() {
        fs.createDirectory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / JsonReporterTest::class.simpleName!!)
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / JsonReporterTest::class.simpleName!!)
    }

    @Test
    fun `should produce valid serialized Report from ordinary data`() {
        val tmpFile = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / JsonReporterTest::class.simpleName!! / "test1"
        val jsonReporter = JsonReporter(fs.sink(tmpFile).buffer())
        jsonReporter.beforeAll()
        jsonReporter.onSuiteStart("suite1")
        jsonReporter.onSuiteEnd("suite1")
        jsonReporter.afterAll()
        jsonReporter.out.flush()

        val reports = Json.decodeFromString<List<Report>>(fs.readFile(tmpFile))
        assertEquals(1, reports.size)
    }
}
