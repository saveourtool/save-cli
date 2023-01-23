/**
 * MPP test Utils for integration tests, especially for downloading of tested tools, like diktat and ktlint
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.core.test.utils

import com.saveourtool.save.core.Save
import com.saveourtool.save.core.config.LogType
import com.saveourtool.save.core.config.OutputStreamType
import com.saveourtool.save.core.config.ReportType
import com.saveourtool.save.core.config.SaveProperties
import com.saveourtool.save.core.files.fs
import com.saveourtool.save.core.logging.logType
import com.saveourtool.save.core.result.Fail
import com.saveourtool.save.core.result.Ignored
import com.saveourtool.save.core.result.Pass
import com.saveourtool.save.reporter.test.TestReporter
import io.kotest.matchers.types.shouldBeTypeOf

import okio.Path

import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @property testName
 * @property reason
 */
data class ExpectedFail(val testName: String, val reason: String)

/**
 * @param testDir `testFiles` as accepted by save-cli
 * @param numberOfTests expected number of executed tests with this configuration
 * @param overrideProperties lambda to override SaveProperties during test
 * @return TestReporter
 */
@Suppress(
    "COMPLEX_EXPRESSION",
    "TOO_LONG_FUNCTION",
)
fun runTestsWithDiktat(
    testDir: List<String>?,
    numberOfTests: Int,
    overrideProperties: (SaveProperties) -> SaveProperties = { it },
): TestReporter {
    val saveProperties = SaveProperties(
        testRootDir = "../examples/kotlin-diktat/",
        testFiles = testDir ?: emptyList(),
        reportType = ReportType.TEST,
        resultOutput = OutputStreamType.STDOUT,
    ).let(overrideProperties)

    // logger is not set from save properties without a config reader, need to set it explicitly
    logType.set(LogType.WARN)
    // In this test we need to merge with emulated empty save.properties file in aim to use default values,
    // since initially all fields are null
    val save = Save(saveProperties, fs)
    val testReporter = save.performAnalysis() as TestReporter

    assertEquals(numberOfTests, testReporter.results.size)
    testReporter.results.forEach { test ->
        // FixMe: if we will have other failing tests - we will make the logic less hardcoded
        if (test.resources.test.name == "ThisShouldAlwaysFailTest.kt") {
            assertEquals(
                Fail(
                    "(MISSING WARNINGS):" +
                            " [Warning(message=[DUMMY_ERROR] this error should not match{{.*}}, line=8, column=1," +
                            " fileName=ThisShouldAlwaysFailTest.kt)]",
                    "(MISSING WARNINGS): (1). (MATCHED WARNINGS): (1)"
                ), test.status
            )
        } else if (test.resources.test.toString().contains("warn${Path.DIRECTORY_SEPARATOR}chapter2")) {
            assertEquals(Fail("ProcessTimeoutException: Timeout is reached: 1", "ProcessTimeoutException: Timeout is reached: 1"), test.status)
        } else {
            assertTrue("test.status is actually ${test.status::class.simpleName}: $test") {
                test.status is Pass || test.status is Ignored
            }
            if (test.status is Ignored) {
                assertEquals(Ignored("Excluded by configuration"), test.status)
            } else {
                test.status.shouldBeTypeOf<Pass>()
            }
        }
    }
    return testReporter
}
