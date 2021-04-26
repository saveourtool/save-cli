package org.cqfn.save.core.result

/**
 * A class that contains non-essential data about test execution, i.e. anything that isn't required to see whether the test
 * succeeds of fails, but may be useful for debugging and gives insights about the tool under test.
 *
 * @property output output of the program under test
 * @property durationMillis duration of execution in milliseconds
 */
data class DebugInfo(
    val output: String?,
    val durationMillis: Long?,
)
