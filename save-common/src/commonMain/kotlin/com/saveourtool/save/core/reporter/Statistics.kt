package com.saveourtool.save.core.reporter

import com.saveourtool.save.core.result.Crash
import com.saveourtool.save.core.result.Fail
import com.saveourtool.save.core.result.Ignored
import com.saveourtool.save.core.result.Pass
import com.saveourtool.save.core.result.TestResult

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
        total++
        when (event.status) {
            is Pass -> passed++
            is Fail -> failed++
            is Ignored -> skipped++
            is Crash -> crashed++
        }
    }
}
