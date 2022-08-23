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
 * @property unmatched number of unmatched checks/validations (warnings) in test (false negative results)
 * @property matched number of matched checks/validations (warnings) in test (true positive results)
 * @property expected number of al checks/validations (warnings) in test (unmatched + matched)
 * @property unexpected number of matched, but unexpected checks/validations (warnings) in test (false positive results)
 */
@Serializable
data class CountWarnings(
    val unmatched: Int,
    val matched: Int,
    val expected: Int,
    val unexpected: Int,
) {
    companion object {
        const val NOT_APPLICABLE_COUNTER: Int = -99

        /**
         * [CountWarnings] is not applicable for current run
         */
        val notApplicable = CountWarnings(
            NOT_APPLICABLE_COUNTER,
            NOT_APPLICABLE_COUNTER,
            NOT_APPLICABLE_COUNTER,
            NOT_APPLICABLE_COUNTER
        )
    }
}
