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

import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineScope

/**
 * Workaround to use suspending functions in unit tests
 *
 * @param block
 * @return simple blocking run to workaround the case with suspend functions
 */
actual fun runTest(block: suspend (scope: CoroutineScope) -> Unit) = runBlocking { block(this) }
