/**
 * MPP test Utils for integration tests, especially for downloading of tested tools, like diktat and ktlint
 */

@file:Suppress("FILE_WILDCARD_IMPORTS")

package org.cqfn.save.core.test.utils

import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.logging.logDebug

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import okio.FileSystem
import okio.Path.Companion.toPath

import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineScope

/**
 * Workaround to use suspending functions in unit tests
 *
 * @param block
 * @return simple blocking run to workaround the case with suspend functions
 */
actual fun runTest(block: suspend (scope: CoroutineScope) -> Unit) = runBlocking { block(this) }

/**
 * @param url
 * @param fileName
 * @return path of the downloaded file
 */
actual suspend fun downloadFile(url: String, fileName: String): String {
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
