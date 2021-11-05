package org.cqfn.save.core.result

import kotlinx.serialization.Serializable

/**
 * A class that contains non-essential data about test execution, i.e. anything that isn't required to see whether the test
 * succeeds of fails, but may be useful for debugging and gives insights about the tool under test.
 *
 * @property execCmd a command used to start static analysis
 * @property stdout output of the program under test from OUT stream
 * @property stderr output of the program under test from ERR stream
 * @property durationMillis duration of execution in milliseconds
 */
@Serializable
data class DebugInfo(
    val execCmd: String?,
    val stdout: String?,
    val stderr: String?,
    val durationMillis: Long?,
)
