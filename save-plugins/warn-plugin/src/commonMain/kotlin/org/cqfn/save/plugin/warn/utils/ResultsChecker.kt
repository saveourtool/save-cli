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
            // matched line and column
            (expected.line == actual.line && expected.column == actual.column) &&
                    // matched text of the message
                    expected.message.createRegexFromMessage().matches(actual.message)
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

    private fun String.createRegexFromMessage(): Regex {
        // patternForRegexInWarning cannot be null, as it has a default value
        val openingDelimiter = warnPluginConfig.patternForRegexInWarning!![0]
        val closingDelimiter = warnPluginConfig.patternForRegexInWarning[1]

        // searching all delimited regex in the warning
        val foundSubStringsWithRegex = this
            .findDelimitedSubStringsWith(openingDelimiter, closingDelimiter)
            .entries
            .sortedBy { it.key }

        val resultWithRegex = foundSubStringsWithRegex.mapIndexed { i, entry ->
            val regexInWarning = this.substring(entry.key, entry.value)
            val endOfTheWarning = this
                .substring(entry.value + closingDelimiter.length, this.length)
                .escapeSpecialRegexSymbols()

            // Example: -> aaa{{.*}}bbb{{.*}}
            when (i) {
                // first regex in the list
                0 -> {
                    // Example: -> aaa
                    val beginningOfTheWarning = this
                        .substring(0, entry.key - openingDelimiter.length)
                        .escapeSpecialRegexSymbols()
                    // Example: -> aaa.*
                    val result = beginningOfTheWarning + regexInWarning
                    // if this regex is the only one in the string, we can simply add the remaining end to it
                    if (foundSubStringsWithRegex.size == 1) result + endOfTheWarning else result
                }
                else -> {
                    // last regex in the list or some value from the middle of the list
                    val result =
                            // Getting part of the string from the end of the previous entry till the current one,
                            // and concatenating it with the regex part
                            // Example: -> bbb + .*
                            this.substring(
                                foundSubStringsWithRegex[i - 1].value + closingDelimiter.length,
                                entry.key - openingDelimiter.length
                            ).escapeSpecialRegexSymbols() + regexInWarning

                    // if the entry is last one in the warning, then adding the tail of the warning and finish
                    if (i == foundSubStringsWithRegex.size - 1) result + endOfTheWarning else result
                }
            }
        }.joinToString()

        // if no regex were found in the string we should simply escape all symbols
        val res = if (foundSubStringsWithRegex.isEmpty()) this.escapeSpecialRegexSymbols() else resultWithRegex

        return Regex(res)
    }

    private fun String.escapeSpecialRegexSymbols() =
            this.replace(regexSymbols) { "\\${it.groupValues[0]}" }

    companion object {
        private const val EXPECTED_BUT_NOT_RECEIVED = "Some warnings were expected but not received"
        private const val UNEXPECTED = "Some warnings were unexpected"
        private val regexSymbols = Regex("[{}()\\[\\].+*?^$\\\\|]")
    }
}
