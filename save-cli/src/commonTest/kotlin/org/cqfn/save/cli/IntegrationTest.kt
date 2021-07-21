package org.cqfn.save.cli

import org.cqfn.save.core.files.readFile
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.utils.ProcessBuilder
import org.cqfn.save.core.utils.isCurrentOsWindows
import org.cqfn.save.reporter.Report

import okio.Path.Companion.toPath

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okio.FileSystem

class IntegrationTest {
    private val fs = FileSystem.SYSTEM

    @Test
    fun `examples test`() {
        val examplesDir = "../examples/kotlin-diktat/"
        val reportFile = examplesDir.toPath() / "save.out.json".toPath()
        if (fs.exists(reportFile)) {
            fs.delete(reportFile)
        }
        val runCmd = if (isCurrentOsWindows()) "" else "./"
        val saveFlags = " --result-output FILE --report-type JSON --test-root-path ."
        val execCmd = "cd $examplesDir && ${runCmd}run.sh $saveFlags"
        ProcessBuilder(true).exec(execCmd, null)

        assertTrue(fs.exists(reportFile))

        val json: List<Report> = Json.decodeFromString(fs.readFile(reportFile))

        fs.delete(reportFile)
        
        json.forEach { report ->
            report.pluginExecutions.forEach { pluginExecution ->
                pluginExecution.testResults.forEach { result ->
                    assertTrue(result.status is Pass)
                }
            }
        }
    }
}
