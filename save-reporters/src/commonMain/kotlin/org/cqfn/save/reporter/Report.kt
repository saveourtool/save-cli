/**
 * Data classes to represent Report of SAVE execution
 */

package org.cqfn.save.reporter

import org.cqfn.save.core.result.TestResult

import kotlinx.serialization.Serializable

/**
 * Report of execution of a test suite
 *
 * @property testSuite name of test suite
 * @property pluginExecutions list of [PluginExecution]
 */
@Serializable
data class Report(
    val testSuite: String,
    val pluginExecutions: List<PluginExecution>,
)

/**
 * Report of single plugin execution
 *
 * @property plugin plugin name
 * @property testResults list of TestResults
 */
@Serializable
data class PluginExecution(
    val plugin: String,
    val testResults: List<TestResult>,
)
