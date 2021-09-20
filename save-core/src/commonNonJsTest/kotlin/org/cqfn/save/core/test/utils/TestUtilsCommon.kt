/**
 * MPP test Utils for integration tests, especially for downloading of tested tools, like diktat and ktlint
 */

@file:Suppress("FILE_WILDCARD_IMPORTS")

package org.cqfn.save.core.test.utils

import org.cqfn.save.core.Save
import org.cqfn.save.core.config.OutputStreamType
import org.cqfn.save.core.config.ReportType
import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.result.Pass
import org.cqfn.save.reporter.test.TestReporter

import okio.FileSystem

import kotlin.test.assertEquals

/**
 * @param testDir `testFiles` as accepted by save-cli
 * @param numberOfTests expected number of executed tests with this configuration
 * @param addProperties lambda to add/override SaveProperties during test
 */
fun runTestsWithDiktat(
    testDir: List<String>?,
    numberOfTests: Int,
    addProperties: SaveProperties.() -> Unit = {}) {
    val mutableTestDir: MutableList<String> = mutableListOf()
    testDir?.let { mutableTestDir.addAll(testDir) }
    mutableTestDir.add(0, "../examples/kotlin-diktat/")
    val saveProperties = SaveProperties(
        testFiles = mutableTestDir,
        reportType = ReportType.TEST,
        resultOutput = OutputStreamType.STDOUT,
    ).apply { addProperties() }
    // In this test we need to merge with emulated empty save.properties file in aim to use default values,
    // since initially all fields are null
    val testReporter = Save(saveProperties.mergeConfigWithPriorityToThis(SaveProperties()), FileSystem.SYSTEM)
        .performAnalysis() as TestReporter

    assertEquals(numberOfTests, testReporter.results.size)
    testReporter.results.forEach {
        assertEquals(Pass(null), it.status)
    }
}
