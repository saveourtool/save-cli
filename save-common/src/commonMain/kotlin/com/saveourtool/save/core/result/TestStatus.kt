/**
 * Classes representing final statuses of test execution
 */

package com.saveourtool.save.core.result

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Base class for final statuses of test execution
 */
@Serializable
sealed class TestStatus

/**
 * @property message Optional message about test passing
 * @property shortMessage Optional message formatted as a short string
 */
@Serializable
data class Pass(val message: String?, val shortMessage: String? = message) : TestStatus()

/**
 * @property reason reason of failure
 * @property shortReason reason of failure formatted as a short string
 */
@Serializable
data class Fail(val reason: String, val shortReason: String) : TestStatus()

/**
 * @property reason reason of test ignoring
 */
@Serializable
data class Ignored(val reason: String) : TestStatus()

/**
 * Represents the case when test execution crashed because of an unhandled internal error in SAVE framework.
 *
 * @property exceptionType type of the exception that caused the crash
 * @property message message from the exception that caused the crash
 */
@Serializable
data class Crash(val exceptionType: String, val message: String) : TestStatus() {
    /**
     * description of an exception that caused crash of SAVE
     */
    @Transient
    val description = "$exceptionType: $message"
}
