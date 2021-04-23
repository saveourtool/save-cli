package org.cqfn.save.core.result

import okio.Path

data class TestResult(
    val resources: Collection<Path>,
    val status: TestStatus,
)
