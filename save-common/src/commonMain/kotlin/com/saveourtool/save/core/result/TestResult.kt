@file:UseSerializers(PathSerializer::class)

package com.saveourtool.save.core.result

import com.saveourtool.save.core.plugin.Plugin
import com.saveourtool.save.core.utils.PathSerializer

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/**
 * Represent results of test execution.
 *
 * @property resources test resources that have been used in this test
 * @property status final status of the test
 * @property debugInfo additional info that can be set during execution. Might be absent.
 */
@Serializable
data class TestResult(
    val resources: Plugin.TestFiles,
    val status: TestStatus,
    val debugInfo: DebugInfo? = null,
)
