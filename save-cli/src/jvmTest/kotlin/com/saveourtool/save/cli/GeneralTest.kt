package com.saveourtool.save.cli

import com.saveourtool.save.core.config.OutputStreamType
import com.saveourtool.save.core.files.StdStreamsSink
import com.saveourtool.save.core.files.readFile
import com.saveourtool.save.core.result.Pass
import com.saveourtool.save.core.utils.CurrentOs
import com.saveourtool.save.core.utils.ProcessBuilder
import com.saveourtool.save.core.utils.getCurrentOs
import com.saveourtool.save.core.utils.isCurrentOsWindows
import com.saveourtool.save.plugins.fix.FixPlugin
import com.saveourtool.save.reporter.Report
import com.saveourtool.save.reporter.json.JsonReporter

import okio.FileNotFoundException
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.decodeFromString

@Suppress(
    "TOO_LONG_FUNCTION",
    "INLINE_CLASS_CAN_BE_USED",
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "ComplexMethod",
)
class GeneralTest {
    private val fs = FileSystem.SYSTEM

    // The `out` property for reporter is basically the stub, just need to create an instance in aim to use json formatter
    private val json = JsonReporter(StdStreamsSink(OutputStreamType.STDOUT).buffer()) {
        FixPlugin.FixTestFiles.register(this)
    }.json

    @Test
    fun `examples test`() {
        // Almost all result statuses should be Pass, except the few cases
        doTest("../examples/kotlin-diktat/") { reports ->
            reports.forEach { report ->
                report.pluginExecutions.forEach { pluginExecution ->
                    pluginExecution.testResults.find { result ->
                        // FixMe: if we will have other failing tests - we will make the logic less hardcoded
                        result.resources.test.name != "GarbageTest.kt" &&
                                result.resources.test.name != "ThisShouldAlwaysFailTest.kt" &&
                                !result.resources.test.toString().contains("warn${Path.DIRECTORY_SEPARATOR}chapter2") &&
                                if (getCurrentOs() == CurrentOs.WINDOWS) {
                                    // These tests fail on Windows: https://github.com/saveourtool/save-cli/issues/402
                                    !result.resources.test.toString().contains("warn-dir${Path.DIRECTORY_SEPARATOR}")
                                } else {
                                    true
                                }
                    }?.let {
                        assertTrue(
                            it.status is Pass,
                            "Test on resources ${it.resources} was expected to pass, but actually has status ${it.status}: $it"
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `examples test from subfolder`() {
        // Almost all result statuses should be Pass, except the few cases
        doTest("../examples/kotlin-diktat/", "fix/smoke") { reports ->
            assertEquals(5, reports.size)
        }
    }

    @Suppress("DEBUG_PRINT")
    private fun doTest(
        workingDir: String,
        testRootDir: String = ".",
        assertReports: (List<Report>) -> Unit
    ) {
        val binDir = "../save-cli/build/bin/" + when (getCurrentOs()) {
            CurrentOs.LINUX -> "linuxX64"
            CurrentOs.MACOS -> "macosX64"
            CurrentOs.WINDOWS -> "mingwX64"
            else -> return
        } + "/debugExecutable"

        assertTrue(fs.exists(binDir.toPath()))

        val saveExecutableFiles = fs.list(binDir.toPath()).filter { fs.metadata(it).isRegularFile }
        // Binary should be created at this moment
        assertTrue(saveExecutableFiles.isNotEmpty())

        val actualSaveBinary = saveExecutableFiles.last()
        val saveBinName = if (isCurrentOsWindows()) "save.exe" else "save"
        val destination = workingDir.toPath() / saveBinName
        // Copy latest version of save into examples
        try {
            fs.copy(actualSaveBinary, destination)
        } catch (fnfe: FileNotFoundException) {
            /*
             * Ignore a potential FNFE on Windows (the destination .exe file may
             * get locked by an external process).
             */
            if (!isCurrentOsWindows()) {
                throw fnfe
            }
        }
        assertTrue(fs.exists(destination))

        // Check for existence of diktat and ktlint
        assertTrue(fs.exists((workingDir.toPath() / "diktat.jar")))
        assertTrue(fs.exists((workingDir.toPath() / "ktlint")))

        // Make sure, that we will check report, which will be obtained after current execution; remove old report if exist
        val reportFile = workingDir.toPath() / "save-reports" / "save.out.json".toPath()
        if (fs.exists(reportFile)) {
            fs.delete(reportFile)
        }

        val runCmd = if (isCurrentOsWindows()) "" else "sudo chmod +x $saveBinName && ./"
        val saveFlags = " $testRootDir --result-output file --report-type json --log all"
        // Execute the script from examples
        val execCmd = "$runCmd$saveBinName $saveFlags"
        val pb = ProcessBuilder(true, fs).exec(execCmd, workingDir, null, 300_000L)
        println("SAVE execution output:\n${pb.stdout.joinToString("\n")}")
        if (pb.stderr.isNotEmpty()) {
            println("Warning and errors during SAVE execution:\n${pb.stderr.joinToString("\n")}")
        }

        // We need some time, before the report will be completely filled
        Thread.sleep(30_000)

        // Report should be created after successful completion
        assertTrue(fs.exists(reportFile))

        val reports: List<Report> = json.decodeFromString(fs.readFile(reportFile))

        println("Following tests failed: ${reports.map { it.testSuite }}")

        assertReports(reports)
        fs.delete(destination)
        fs.delete(reportFile)
    }
}
