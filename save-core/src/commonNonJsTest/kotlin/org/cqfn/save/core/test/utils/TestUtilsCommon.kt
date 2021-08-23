/**
 * MPP test Utils for integration tests, especially for downloading of tested tools, like diktat and ktlint
 */

@file:Suppress("FILE_WILDCARD_IMPORTS")

package org.cqfn.save.core.test.utils

import org.cqfn.save.core.Save
import org.cqfn.save.core.config.OutputStreamType
import org.cqfn.save.core.config.ReportType
import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.result.Pass
import org.cqfn.save.reporter.test.TestReporter

import io.ktor.client.*
import io.ktor.client.call.receive
import io.ktor.client.features.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import okio.FileSystem
import okio.Path.Companion.toPath

import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope

const val KTLINT_VERSION = "0.39.0"
const val DIKTAT_VERSION = "1.0.0-rc.2"
const val TIMEOUT = 100_000L

/**
 * Workaround to use suspending functions in unit tests
 *
 * @param block
 */
expect fun runTest(block: suspend (scope: CoroutineScope) -> Unit)

/**
 * Download file from [url] into [fileName]
 *
 * @param url url to download from
 * @param fileName name of the downloaded file
 * @return path to the downloaded file as a string
 */
suspend fun downloadFile(url: String, fileName: String): String {
    val fs = FileSystem.SYSTEM
    val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = TIMEOUT
            connectTimeoutMillis = TIMEOUT
            socketTimeoutMillis = TIMEOUT
        }
    }

    val file = fileName.toPath()
    if (!fs.exists(file)) {
        val httpResponse: HttpResponse = client.get(url)
        val responseBody: ByteArray = httpResponse.receive()
        logDebug("Writing ${responseBody.size} bytes into $file")
        val newPath = fs.createFile(fileName)
        fs.write(newPath) {
            write(responseBody)
        }
        logDebug("$url downloaded to $file")
    }

    client.close()
    return file.toString()
}

/**
 * @param testDir `testFiles` as accepted by save-cli
 * @param numberOfTests expected number of executed tests with this configuration
 * @param addProperties lambda to add/override SaveProperties during test
 */
suspend fun runTestsWithDiktat(
    testDir: List<String>?,
    numberOfTests: Int,
    addProperties: SaveProperties.() -> Unit = {}) {
    downloadFile(
        "https://github.com/pinterest/ktlint/releases/download/$KTLINT_VERSION/ktlint",
        "../examples/kotlin-diktat/ktlint"
    )

    downloadFile(
        "https://github.com/cqfn/diKTat/releases/download/v$DIKTAT_VERSION/diktat-$DIKTAT_VERSION.jar",
        "../examples/kotlin-diktat/diktat.jar"
    )

    val saveProperties = SaveProperties(
        testFiles = testDir,
        testRootPath = "../examples/kotlin-diktat/",
        reportType = ReportType.TEST,
        resultOutput = OutputStreamType.STDOUT,
    ).apply { addProperties() }
    // In this test we need to merge with emulated empty save.properties file in aim to use default values,
    // since initially all fields are null
    // In this test we need to merge with emulated empty save.properties file in aim to use default values,
    // since initially all fields are null
    val testReporter = Save(saveProperties.mergeConfigWithPriorityToThis(SaveProperties()), FileSystem.SYSTEM)
        .performAnalysis() as TestReporter

    assertEquals(numberOfTests, testReporter.results.size)
    testReporter.results.forEach {
        assertEquals(Pass(null), it.status)
    }
}
