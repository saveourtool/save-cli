package org.cqfn.save.cli

import org.cqfn.save.core.files.readFile
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.utils.CurrentOs
import org.cqfn.save.core.utils.ProcessBuilder
import org.cqfn.save.core.utils.getCurrentOs
import org.cqfn.save.core.utils.isCurrentOsWindows
import org.cqfn.save.plugins.fix.FixPlugin
import org.cqfn.save.reporter.Report

import okio.FileSystem
import okio.Path.Companion.toPath

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Suppress(
    "TOO_LONG_FUNCTION",
    "INLINE_CLASS_CAN_BE_USED",
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS"
)
@OptIn(ExperimentalSerializationApi::class)
class GeneralTest {
    private val fs = FileSystem.SYSTEM
    private val json = Json {
        serializersModule = SerializersModule {
            polymorphic(Plugin.TestFiles::class) {
                subclass(Plugin.Test::class)
                subclass(FixPlugin.FixTestFiles::class)
            }
        }
    }

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
        val destination = examplesDir.toPath() / saveBinName
        // Copy latest version of save into examples
        fs.copy(actualSaveBinary, destination)
        assertTrue(fs.exists(destination))

        // Check for existence of diktat and ktlint
        assertTrue(fs.exists((examplesDir.toPath() / "diktat.jar")))
        assertTrue(fs.exists((examplesDir.toPath() / "ktlint")))

        // Make sure, that we will check report, which will be obtained after current execution; remove old report if exist
        val reportFile = examplesDir.toPath() / "save.out.json".toPath()
        if (fs.exists(reportFile)) {
            fs.delete(reportFile)
        }

        val runCmd = if (isCurrentOsWindows()) "" else "sudo chmod +x $saveBinName && ./"
        val saveFlags = " . --result-output FILE --report-type JSON"
        // Execute the script from examples
        val execCmd = "$runCmd$saveBinName $saveFlags"
        val pb = ProcessBuilder(true, fs).exec(execCmd, examplesDir, null)
        println("SAVE execution output:\n${pb.stdout.joinToString("\n")}")
        if (pb.stderr.isNotEmpty()) {
            println("Warning and errors during SAVE execution:\n${pb.stderr.joinToString("\n")}")
        }

        // We need some time, before the report will be completely filled
        Thread.sleep(20_000)

        // Report should be created after successful completion
        assertTrue(fs.exists(reportFile))

        val reports: List<Report> = json.decodeFromString(fs.readFile(reportFile))
        // All result statuses should be Pass
        reports.forEach { report ->
            report.pluginExecutions.forEach { pluginExecution ->
                pluginExecution.testResults.find { result ->
                    println(result.status)
                    result.resources.test.name != "ThisShouldAlwaysFailTest.kt"
                }?.let {
                    assertTrue(it.status is Pass)
                }
            }
        }
        fs.delete(destination)
        fs.delete(reportFile)
    }
}
