@file:UseSerializers(PathSerializer::class)

package org.cqfn.save.core.result

import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.utils.PathSerializer

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/**
 * Represent results of test execution.
 *
 * @property resources test resources that have been used in this test
 * @property status final status of the test
 * @property debugInfo additional info that can be set during execution. Might be absent.
 * @property missing number of missing warnings
 * @property match number of match warnings
 */
@Serializable
data class TestResult(
    val resources: Plugin.TestFiles,
    val status: TestStatus,
    val debugInfo: DebugInfo? = null,
    val missing: Int? = null,
    val match: Int? = null,
)
