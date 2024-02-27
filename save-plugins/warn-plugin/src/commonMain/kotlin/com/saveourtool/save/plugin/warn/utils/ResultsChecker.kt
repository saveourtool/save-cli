package com.saveourtool.save.plugin.warn.utils

import com.saveourtool.save.core.logging.logError
import com.saveourtool.save.core.result.CountWarnings
import com.saveourtool.save.core.result.Fail
import com.saveourtool.save.core.result.Pass
import com.saveourtool.save.core.result.TestStatus
import com.saveourtool.save.plugin.warn.WarnPluginConfig

typealias FileToWarningsMap = Map<String, List<Warning>?>

/**
 * Class that supports checking of the test results
 *
 * @property expectedWarningsMap expected warnings, grouped by LineCol
 * @property actualWarningsMap actual warnings, grouped by LineCol
 * @property warnPluginConfig configuration of warn plugin
 **/
class ResultsChecker(
    private val expectedWarningsMap: FileToWarningsMap,
    private val actualWarningsMap: FileToWarningsMap,
    private val warnPluginConfig: WarnPluginConfig,
) {
    init {
        val oddActualFileNames = actualWarningsMap.keys - expectedWarningsMap.keys
        if (oddActualFileNames.isNotEmpty()) {
            logError("Detected odd file names in actual result: $oddActualFileNames")
        }
    }

    /**
     * Compares actual and expected warnings and returns TestResult
     *
     * @param testFileName
     * @return [TestStatus]
     */
    @Suppress("TYPE_ALIAS")
    internal fun checkResults(testFileName: String): Pair<TestStatus, CountWarnings> {
        val actualWarnings = actualWarningsMap[testFileName] ?: listOf()
        val expectedWarnings = expectedWarningsMap[testFileName] ?: listOf()

        // Actual warnings that were found among of Expected warnings (will be filled in matchWithActualWarnings())
        val actualMatchedWithExpectedWarnings: MutableList<Warning> = mutableListOf()
        // Expected warnings that were found among of Actual warnings
        val expectedWarningsMatchedWithActual = expectedWarnings
            .matchWithActualWarnings(actualWarnings, actualMatchedWithExpectedWarnings)

        val missingWarnings = expectedWarnings - expectedWarningsMatchedWithActual
        val unexpectedWarnings = actualWarnings - actualMatchedWithExpectedWarnings

        val countWarnings = CountWarnings(
            unmatched = missingWarnings.size,
            matched = expectedWarningsMatchedWithActual.size,
            expected = expectedWarnings.size,
            unexpected = unexpectedWarnings.size
        )

        return when (missingWarnings.isEmpty() to unexpectedWarnings.isEmpty()) {
            false to true -> Fail(
                "$MISSING $missingWarnings",
                "$MISSING (${countWarnings.unmatched}). $MATCHED (${countWarnings.matched})"
            )
            false to false -> createFailFromDoubleMiss(missingWarnings, unexpectedWarnings, expectedWarningsMatchedWithActual)
            true to true -> Pass("$ALL_EXPECTED (${countWarnings.matched})")
            true to false -> if (warnPluginConfig.exactWarningsMatch == false) {
                Pass(
                    "$UNEXPECTED $unexpectedWarnings",
                    "$UNEXPECTED (${countWarnings.unexpected}). $MATCHED (${countWarnings.matched})"
                )
            } else {
                Fail(
                    "$UNEXPECTED $unexpectedWarnings",
                    "$UNEXPECTED (${countWarnings.unexpected}). $MATCHED (${countWarnings.matched})"
                )
            }
            else -> Fail("N/A", "N/A")
        } to countWarnings
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
                    expected.message
                        .createRegexFromString(
                            openingDelimiter,
                            closingDelimiter,
                            warnPluginConfig.partialWarnTextMatch ?: false
                        )
                        .matches(actual.message)
        }

        matchedWarning?.let {
            actualMatchedWithExpectedWarnings.add(matchedWarning)
        }
        matchedWarning != null
    }

    private fun createFailFromDoubleMiss(
        missingWarnings: List<Warning>,
        unexpectedWarnings: List<Warning>,
        matchedWarnings: List<Warning>
    ) = Fail(
        "$MISSING $missingWarnings. $UNEXPECTED $unexpectedWarnings.",
        "$MISSING (${missingWarnings.size}). " +
                "$UNEXPECTED (${unexpectedWarnings.size}). " +
                "$MATCHED (${matchedWarnings.size})"
    )

    companion object {
        private const val ALL_EXPECTED = "(ALL WARNINGS MATCHED):"
        private const val MATCHED = "(MATCHED WARNINGS):"
        private const val MISSING = "(MISSING WARNINGS):"
        private const val UNEXPECTED = "(UNEXPECTED WARNINGS):"
    }
}
