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
        expectedWarningsMap[testFileName]?.map { it.message.createRegexFromMessage() }

        val missingWarnings = (expectedWarningsMap[testFileName] ?: listOf()) - (actualWarningsMap[testFileName] ?: listOf())
        val unexpectedWarnings = (actualWarningsMap[testFileName] ?: listOf()) -((expectedWarningsMap[testFileName] ?: listOf()))

        return when (missingWarnings.isEmpty() to unexpectedWarnings.isEmpty()) {
            false to true -> createFailFromSingleMiss(EXPECTED_BUT_NOT_RECEIVED, missingWarnings)
            false to false -> createFailFromDoubleMiss(missingWarnings, unexpectedWarnings)
            true to true -> Pass(null)
            true to false -> if (warnPluginConfig.exactWarningsMatch == false) {
                Pass("$UNEXPECTED: $unexpectedWarnings", "$UNEXPECTED: ${unexpectedWarnings.size} warnings")
            } else {
                createFailFromSingleMiss(UNEXPECTED, unexpectedWarnings)
            }
            else -> Fail("N/A", "N/A")
        }
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

        val delimitedSubStrings = this
            .findDelimitedSubStringsWith(openingDelimiter, closingDelimiter)
            .entries
            .sortedBy { it.key }

        val result = delimitedSubStrings.mapIndexed {
                i, entry ->
                // how to escape symbols in the non-regex part?
                // what if opening symbol is first and closing is last?
                when(i) {
                    0 -> {
                        if (delimitedSubStrings.size == 1) {
                            this.substring(0, entry.key - openingDelimiter.length) +
                            this.substring(entry.key, entry.value) +
                            this.substring(entry.value + closingDelimiter.length, this.length)
                        } else {
                            this.substring(0, entry.key - openingDelimiter.length) +
                            this.substring(entry.key, entry.value)
                        }
                    }
                    delimitedSubStrings.size - 1 -> {
                        this.substring(delimitedSubStrings[i - 1].value + closingDelimiter.length, entry.key - openingDelimiter.length) +
                        this.substring(entry.key, entry.value) +
                        this.substring(entry.value + closingDelimiter.length, this.length)
                    }
                    else -> {
                        this.substring(delimitedSubStrings[i - 1].value + closingDelimiter.length, entry.key - openingDelimiter.length) +
                        this.substring(entry.key, entry.value)
                    }
                }
            }

        println(result)

        return Regex("")
    }


    companion object {
        private const val EXPECTED_BUT_NOT_RECEIVED = "Some warnings were expected but not received"
        private const val UNEXPECTED = "Some warnings were unexpected"
    }
}