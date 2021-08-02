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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

/**
 * Workaround to use suspending functions in unit tests
 *
 * @param block
 * @return the result of blocked suspend function
 */
actual fun runTest(block: suspend (scope: CoroutineScope) -> Unit) = runBlocking { block(this) }

@Suppress("MISSING_KDOC_ON_FUNCTION", "MISSING_KDOC_TOP_LEVEL")  // https://github.com/cqfn/diKTat/issues/1012
actual suspend fun createHttpClient(): HttpClient = HttpClient {
    install(HttpTimeout) {
        requestTimeoutMillis = TIMEOUT
        connectTimeoutMillis = TIMEOUT
        socketTimeoutMillis = TIMEOUT
    }
}
