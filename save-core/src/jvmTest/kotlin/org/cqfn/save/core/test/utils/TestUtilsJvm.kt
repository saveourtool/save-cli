/**
 * MPP test Utils for integration tests, especially for downloading of tested tools, like diktat and ktlint
 */

@file:Suppress("FILE_WILDCARD_IMPORTS")

package org.cqfn.save.core.test.utils

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

import java.io.File

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

/**
 * Workaround to use suspending functions in unit tests
 *
 * @param block
 * @return the result of blocked suspend function
 */
actual fun runTest(block: suspend (scope: CoroutineScope) -> Unit) = runBlocking { block(this) }

/**
 * @param url
 * @param fileName
 * @return path of the downloaded file
 */
actual suspend fun downloadFile(url: String, fileName: String): String {
    val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = TIMEOUT
            connectTimeoutMillis = TIMEOUT
            socketTimeoutMillis = TIMEOUT
        }
    }

    val file = File(fileName)
    if (!file.exists()) {
        val httpResponse: HttpResponse = client.get(url)
        val responseBody: ByteArray = httpResponse.receive()
        println("Writing ${responseBody.size} bytes into ${file.path}")
        file.writeBytes(responseBody)
        println("$url downloaded to ${file.path}")
    }

    client.close()
    return file.absolutePath
}
