package org.cqfn.save.core.test.utils
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Workaround to use suspending functions in unit tests
 */
actual fun runTest(block: suspend (scope : CoroutineScope) -> Unit) = runBlocking { block(this) }

actual suspend fun downloadFile(url: String, fileName: String): String {
    val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 100000
            connectTimeoutMillis = 100000
            socketTimeoutMillis = 100000
        }
    }

    val file = File(fileName)
    if (!file.exists()) {
            val httpResponse: HttpResponse = client.get(url)
            val responseBody: ByteArray = httpResponse.receive()
            file.writeBytes(responseBody)
            println("$fileName downloaded to ${file.path}")
            client.close()
    }

    return file.absolutePath
}
