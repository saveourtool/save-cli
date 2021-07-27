package org.cqfn.save.core.test.utils

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.*
import okio.FileSystem
import org.cqfn.save.core.files.createFile


/**
 * Workaround to use suspending functions in unit tests
 */
actual fun runTest(block: suspend (scope : CoroutineScope) -> Unit) = runBlocking { block(this) }

actual suspend fun downloadFile(url: String, fileName: String): String {
    val fs = FileSystem.SYSTEM

    val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 100000
            connectTimeoutMillis = 100000
            socketTimeoutMillis = 100000
        }
    }

    val file = fs.createFile(fileName)

    if (!fs.exists(file)) {
        val httpResponse: HttpResponse = client.get(url)
        val responseBody: ByteArray = httpResponse.receive()
        fs.write(file) {
            responseBody
        }
        println("$fileName downloaded to ${file}")
        client.close()
    }

    return file.toString()
}
