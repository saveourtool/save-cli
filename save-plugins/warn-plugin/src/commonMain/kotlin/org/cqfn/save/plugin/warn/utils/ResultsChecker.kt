package org.cqfn.save.plugin.warn.utils

import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.core.result.TestStatus
import org.cqfn.save.plugin.warn.WarnPluginConfig

class ResultsChecker(
    private val expectedWarningsMap: Map<String, List<Warning>?>,
    private val actualWarningsMap: Map<String, List<Warning>?>,
    private val warnPluginConfig: WarnPluginConfig,
) {
    /**
     * Compares actual and expected warnings and returns TestResult
     *
     * @param expectedWarningsMap expected warnings, grouped by LineCol
     * @param actualWarningsMap actual warnings, grouped by LineCol
     * @param warnPluginConfig configuration of warn plugin
     * @return [TestResult]
     */
    @Suppress("TYPE_ALIAS")
    internal fun checkResults(testFileName: String): TestStatus {

        val actualWarnings = actualWarningsMap[testFileName] ?: listOf()
        val expectedWarnings = expectedWarningsMap[testFileName] ?: listOf()

        // Actual warnings that were found among of Expected warnings (will be filled in matchWithActualWarnings())
        val actualMatchedWithExpectedWarnings = mutableListOf<Warning>()
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

        if (matchedWarning != null) actualMatchedWithExpectedWarnings.add(matchedWarning)
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
        val foundDelimitedSubStringsWithRegex = this
            .findDelimitedSubStringsWith(openingDelimiter, closingDelimiter)
            .entries
            .sortedBy { it.key }

        val resultWithRegex = foundDelimitedSubStringsWithRegex.mapIndexed { i, entry ->
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
                    if (foundDelimitedSubStringsWithRegex.size == 1) result + endOfTheWarning else result
                }
                else -> {
                    // last regex in the list or some value from the middle of the list
                    val result =
                    // Getting part of the string from the end of the previous entry till the current one,
                    // and concatenating it with the regex part
                        // Example: -> bbb + .*
                        this.substring(
                            foundDelimitedSubStringsWithRegex[i - 1].value + closingDelimiter.length,
                            entry.key - openingDelimiter.length
                        ).escapeSpecialRegexSymbols() + regexInWarning

                    // if the entry is last one in the warning, then adding the tail of the warning and finish
                    if (i == foundDelimitedSubStringsWithRegex.size - 1) result + endOfTheWarning else result
                }
            }
        }

        // if no regex were found in the string we should simply escape all symbols
        val result =
            if (foundDelimitedSubStringsWithRegex.isEmpty()) this.escapeSpecialRegexSymbols() else resultWithRegex.joinToString(
                ""
            )
        return Regex(result)
    }

    private fun String.escapeSpecialRegexSymbols() =
        this.replace(REGEX_SYMBOLS) { "\\${it.groupValues[0]}" }

    companion object {
        private const val EXPECTED_BUT_NOT_RECEIVED = "Some warnings were expected but not received"
        private const val UNEXPECTED = "Some warnings were unexpected"
        private val REGEX_SYMBOLS = Regex("[{}()\\[\\].+*?^$\\\\|]")
    }
}
