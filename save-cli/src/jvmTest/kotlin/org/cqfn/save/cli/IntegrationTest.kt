package org.cqfn.save.cli

import org.cqfn.save.core.files.readFile
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.utils.CurrentOs
import org.cqfn.save.core.utils.ProcessBuilder
import org.cqfn.save.core.utils.getCurrentOs
import org.cqfn.save.core.utils.isCurrentOsWindows
import org.cqfn.save.reporter.Report

import okio.FileSystem
import okio.Path.Companion.toPath

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Suppress(
    "TOO_LONG_FUNCTION",
    "INLINE_CLASS_CAN_BE_USED",
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS")
@OptIn(ExperimentalSerializationApi::class)
class IntegrationTest {
    private val fs = FileSystem.SYSTEM

    @Test
    fun `examples test`() {
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

        val examplesDir = "../examples/kotlin-diktat/"

        val actualSaveBinary = saveExecutableFiles.last()
        val saveBinName = if (isCurrentOsWindows()) "save.exe" else "save"
        val destination = (examplesDir + saveBinName).toPath()

        // Copy latest version of save into examples
        fs.copy(actualSaveBinary, destination)
        assertTrue(fs.exists(destination))

        // Check for existence of diktat and ktlint
        assertTrue(fs.exists((examplesDir + "diktat.jar").toPath()))
        assertTrue(fs.exists((examplesDir + "ktlint").toPath()))

        // Make sure, that we will check report, which will be obtained after current execution; remove old report if exist
        val reportFile = examplesDir.toPath() / "save.out.json".toPath()
        if (fs.exists(reportFile)) {
            fs.delete(reportFile)
        }

        val runCmd = if (isCurrentOsWindows()) "" else "sudo chmod +x $saveBinName && sudo ./"
        val saveFlags = " . --result-output FILE --report-type JSON"
        // Execute the script from examples
        val execCmd = "$runCmd$saveBinName $saveFlags"
        ProcessBuilder(true, fs).exec(execCmd, examplesDir, null)

        Thread.sleep(15_000)

        // Report should be created after successful completion
        assertTrue(fs.exists(reportFile))

        val json: List<Report> = Json.decodeFromString(fs.readFile(reportFile))

        // All result statuses should be Pass
        json.forEach { report ->
            report.pluginExecutions.forEach { pluginExecution ->
                pluginExecution.testResults.forEach { result ->
                    println(result.status)
                    assertTrue(result.status is Pass)
                }
            }
        }
        fs.delete(destination)
        fs.delete(reportFile)
    }
}
