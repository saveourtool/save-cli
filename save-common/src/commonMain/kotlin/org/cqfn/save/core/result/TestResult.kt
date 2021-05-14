package org.cqfn.save.core.result

import okio.Path

/**
 * Represent results of test execution.
 *
 * @property resources test resources that have been used in this test
 * @property status final status of the test
 * @property debugInfo additional info that can be set during execution. Might be absent.
 */
data class TestResult(
    val resources: Collection<Path>,
    val status: TestStatus,
    val debugInfo: DebugInfo? = null,
)
