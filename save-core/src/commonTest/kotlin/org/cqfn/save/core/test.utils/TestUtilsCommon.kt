package org.cqfn.save.core.test.utils
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import okio.Path.Companion.toPath
import org.cqfn.save.core.Save
import org.cqfn.save.core.config.OutputStreamType
import org.cqfn.save.core.config.ReportType
import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.result.Pass
import org.cqfn.save.reporter.test.TestReporter
import kotlin.test.assertEquals

const val KTLINT_VERSION = "0.39.0"
const val DIKTAT_VERSION = "1.0.0-rc.2"

/**
 * Workaround to use suspending functions in unit tests
 */
expect fun runTest(block: suspend (scope : CoroutineScope) -> Unit)

expect suspend fun downloadFile(url: String, fileName: String): String

suspend fun runTestsWithDiktat(testDir: List<String>, numberOfTests: Int) {
    downloadFile(
        "https://github.com/pinterest/ktlint/releases/download/${org.cqfn.save.core.integration.KTLINT_VERSION}/ktlint",
        "../examples/kotlin-diktat/ktlint"
    )

    downloadFile(
        "https://github.com/cqfn/diKTat/releases/download/v${org.cqfn.save.core.integration.DIKTAT_VERSION}/diktat-${org.cqfn.save.core.integration.DIKTAT_VERSION}.jar",
        "../examples/kotlin-diktat/diktat.jar"
    )

    val saveProperties = SaveProperties(
        testFiles = testDir,
        testRootPath = "../examples/kotlin-diktat/",
        reportType = ReportType.TEST,
        resultOutput = OutputStreamType.STDOUT,
    )
    // In this test we need to merge with emulated empty save.properties file in aim to use default values,
    // since initially all fields are null
    // In this test we need to merge with emulated empty save.properties file in aim to use default values,
    // since initially all fields are null
    val testReporter = Save(saveProperties.mergeConfigWithPriorityToThis(SaveProperties()))
        .performAnalysis() as TestReporter

    assertEquals(numberOfTests, testReporter.results.size)
    testReporter.results.forEach {
        assertEquals(Pass(null), it.status)
    }
}
