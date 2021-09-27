package org.cqfn.save.plugin.warn.utils

import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestStatus
import org.cqfn.save.plugin.warn.WarnPluginConfig

typealias FileToWarningsMap = Map<String, List<Warning>?>

/**
 * Class that supports checking of the test results
 *
 * @param expectedWarningsMap expected warnings, grouped by LineCol
 * @param actualWarningsMap actual warnings, grouped by LineCol
 * @param warnPluginConfig configuration of warn plugin
 **/
class ResultsChecker(
    private val expectedWarningsMap: FileToWarningsMap,
    private val actualWarningsMap: FileToWarningsMap,
    private val warnPluginConfig: WarnPluginConfig,
) {
    /**
     * Compares actual and expected warnings and returns TestResult
     *
     * @param testFileName
     * @return [TestStatus]
     */
    @Suppress("TYPE_ALIAS")
    internal fun checkResults(testFileName: String): TestStatus {
        val actualWarnings = actualWarningsMap[testFileName] ?: listOf()
        val expectedWarnings = expectedWarningsMap[testFileName] ?: listOf()

        // Actual warnings that were found among of Expected warnings (will be filled in matchWithActualWarnings())
        val actualMatchedWithExpectedWarnings: MutableList<Warning> = mutableListOf()
        // Expected warnings that were found among of Actual warnings
        val expectedWarningsMatchedWithActual = expectedWarnings
            .matchWithActualWarnings(actualWarnings, actualMatchedWithExpectedWarnings)

        val missingWarnings = expectedWarnings - expectedWarningsMatchedWithActual
        val unexpectedWarnings = actualWarnings - actualMatchedWithExpectedWarnings

        return when (missingWarnings.isEmpty() to unexpectedWarnings.isEmpty()) {
            false to true -> createFailFromSingleMiss(EXPECTED_BUT_NOT_RECEIVED, missingWarnings)
            false to false -> createFailFromDoubleMiss(missingWarnings, unexpectedWarnings)
            true to true -> Pass(null)
            true to false -> if (warnPluginConfig.exactWarningsMatch == false) {
                Pass(
                    "$UNEXPECTED: $unexpectedWarnings",
                    "$UNEXPECTED: ${unexpectedWarnings.size} warnings"
                )
            } else {
                createFailFromSingleMiss(UNEXPECTED, unexpectedWarnings)
            }
            else -> Fail("N/A", "N/A")
        }
    }

    private fun List<Warning>.matchWithActualWarnings(
        actualWarnings: List<Warning>,
        actualMatchedWithExpectedWarnings: MutableList<Warning>
    ) = this.filter { expected ->
        val matchedWarning = actualWarnings.find { actual ->
            // patternForRegexInWarning cannot be null, as it has a default value
            val openingDelimiter = warnPluginConfig.patternForRegexInWarning!![0]
            val closingDelimiter = warnPluginConfig.patternForRegexInWarning[1]
            // matched line and column
            (expected.line == actual.line && expected.column == actual.column) &&
                    // matched text of the message
                    expected.message.createRegexFromString(openingDelimiter, closingDelimiter).matches(actual.message)
        }

        matchedWarning?.let {
            actualMatchedWithExpectedWarnings.add(matchedWarning)
        }
        matchedWarning != null
    }

    private fun createFailFromSingleMiss(baseText: String, warnings: List<Warning>) =
            Fail("$baseText: $warnings", "$baseText (${warnings.size})")

    private fun createFailFromDoubleMiss(missingWarnings: List<Warning>, unexpectedWarnings: List<Warning>) =
            Fail(
                "$EXPECTED_BUT_NOT_RECEIVED: $missingWarnings, and ${UNEXPECTED.lowercase()}: $unexpectedWarnings",
                "$EXPECTED_BUT_NOT_RECEIVED (${missingWarnings.size}), and ${UNEXPECTED.lowercase()} (${unexpectedWarnings.size})"
            )

    companion object {
        private const val EXPECTED_BUT_NOT_RECEIVED = "Some warnings were expected but not received"
        private const val UNEXPECTED = "Some warnings were unexpected"
    }
}
