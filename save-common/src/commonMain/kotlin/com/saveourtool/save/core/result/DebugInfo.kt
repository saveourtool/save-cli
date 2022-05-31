@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.core.result

import kotlinx.serialization.Serializable

/**
 * A class that contains non-essential data about test execution, i.e. anything that isn't required to see whether the test
 * succeeds of fails, but may be useful for debugging and gives insights about the tool under test.
 *
 * @property execCmd a command used to start static analysis
 * @property stdout output of the program under test from OUT stream
 * @property stderr output of the program under test from ERR stream
 * @property durationMillis duration of execution in milliseconds
 * @property countWarnings number of missing and match warnings
 */
@Serializable
data class DebugInfo(
    val execCmd: String?,
    val stdout: String?,
    val stderr: String?,
    val durationMillis: Long?,
    val countWarnings: CountWarnings? = null,
)

/**
 * @property missing number of missing warnings
 * @property match number of match warnings
 * @property expected number of expected warnings
 * @property unexpectedMatch number of unexpected match warnings
 */
@Serializable
data class CountWarnings(
    val missing: Int,
    val match: Int,
    val expected: Int,
    val unexpectedMatch: Int,
)
