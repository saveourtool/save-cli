/**
 * Classes representing final statuses of test execution
 */

package org.cqfn.save.core.result

/**
 * Base class for final statuses of test execution
 */
sealed class TestStatus

object Success : TestStatus()

/**
 * @property reason reason of failure
 */
data class Failure(val reason: String) : TestStatus()

/**
 * @property reason reason of test ignoring
 */
data class Ignored(val reason: String) : TestStatus()

/**
 * Represents the case when test execution crashed because of an unhandled internal error in SAVE framework.
 *
 * @property throwable an exception that caused crash of SAVE
 */
data class Crash(val throwable: Throwable) : TestStatus()
