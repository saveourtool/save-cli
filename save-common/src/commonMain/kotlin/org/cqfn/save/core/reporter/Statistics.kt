package org.cqfn.save.core.reporter

import org.cqfn.save.core.result.Crash
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Ignored
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult

/**
 * Container for statistics about executed tests
 */
class Statistics {
    /**
     * Total number of tests
     */
    var total: Int = 0

    /**
     * Number of passed tests
     */
    var passed: Int = 0

    /**
     * Number of failed tests
     */
    var failed: Int = 0

    /**
     * Number of skipped tests
     */
    var skipped: Int = 0

    /**
     * Number of tests which have crashed
     */
    var crashed: Int = 0

    /**
     * Update fields of this class based on the received [TestResult]
     *
     * @param event an event to update statistics
     */
    @Suppress("WHEN_WITHOUT_ELSE")
    fun updateFrom(event: TestResult) {
        total += 1
        when (event.status) {
            is Pass -> passed += 1
            is Fail -> failed += 1
            is Ignored -> skipped += 1
            is Crash -> crashed += 1
        }
    }
}
