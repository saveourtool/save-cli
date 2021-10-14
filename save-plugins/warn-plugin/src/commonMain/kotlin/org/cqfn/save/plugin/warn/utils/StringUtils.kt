/**
 * Very specific string utility extensions for getting specific substrings
 */

package org.cqfn.save.plugin.warn.utils

private val delimiterNotFound = Pair(-1, -1)

/**
 * Finding indexes of all delimited groups.
 * For example, if openingDelimiter = ( , closingDelimiter = ) , this = aaa(bbb)ccc(eee) =>
 * method will return indexes of the delimited string (start inclusive, end exclusive) - bbb - map(4 = 6, 12 = 14), counted from 0.
 *
 * @param openingDelimiter opening group of symbols that is used to separate pattern
 * @param closingDelimiter closing group of symbols that is used to separate pattern
 * @return map with indexes of the delimited string
 */
fun String.findDelimitedSubStringsWith(openingDelimiter: String, closingDelimiter: String): MutableMap<Int, Int> {
    // finding first delimited group in the initial string
    var delimitedGroup = this.findFirstDelimitedSubStringBy(openingDelimiter, closingDelimiter)
    var nextPartOfString = this
    val result = if (delimitedGroup != delimiterNotFound) mutableMapOf(delimitedGroup) else mutableMapOf()
    while (true) {
        // offset that is used to restore indexes in the initial string (as the logic below is applied to substring)
        val offset = delimitedGroup.second + closingDelimiter.length
        // substring without a part of a string that was already processed (shifted by offset)
        nextPartOfString = nextPartOfString.substring(offset, nextPartOfString.length)
        delimitedGroup = nextPartOfString.findFirstDelimitedSubStringBy(openingDelimiter, closingDelimiter)
        // no need to process if there are no delimiters in the substring
        if (delimitedGroup == delimiterNotFound) {
            break
        }
        result[offset + delimitedGroup.first] = offset + delimitedGroup.second
    }

    return result
}

/**
 * Finding the first group of strings that is delimited by openingDelimiter and closingDelimiter.
 * For example, if openingDelimiter = ( , closingDelimiter = ) , this = aaa(bbb)ccc(eee) =>
 * method will return indexes delimited string (start inclusive, end exclusive) - bbb - Pair(4, 6), counted from 0.
 *
 * Returns DELIMITER_NOT_FOUND - Pair(-1, -1) if no opening/closing group found
 *
 * @param openingDelimiter opening group of symbols that is used to separate pattern
 * @param closingDelimiter closing group of symbols that is used to separate pattern
 * @return map with indexes of the delimited string
 * @throws IllegalArgumentException in case of invalid string and invalid delimiters
 */
fun String.findFirstDelimitedSubStringBy(openingDelimiter: String, closingDelimiter: String): Pair<Int, Int> {
    val foundOpenSymbols = this.indexOf(openingDelimiter)
    val foundClosingSymbols = this.indexOf(closingDelimiter)

    when {
        // not found any regular expressions in the warning
        foundOpenSymbols == -1 && foundClosingSymbols == -1 -> return delimiterNotFound
        // closing symbols stay before opening symbols OR no opening symbols found for closing symbols
        foundClosingSymbols < foundOpenSymbols -> throw IllegalArgumentException(
            "Not able to find delimited substrings in \" $this \" that are surrounded by '$openingDelimiter'" +
                    " and '$closingDelimiter' because closing delimiter '$closingDelimiter' (index =" +
                    " $foundClosingSymbols) is placed before the opening delimiter" +
                    " '$openingDelimiter' (index = $foundOpenSymbols)."
        )
        // opening symbols found without closing symbols
        foundClosingSymbols != -1 && foundOpenSymbols == -1 -> throw IllegalArgumentException(
            " Not able to find delimited substrings in \" $this \" that are surrounded by '$openingDelimiter'" +
                    " and '$closingDelimiter' because '$closingDelimiter' is missing while the" +
                    " '$openingDelimiter' (index = '$foundOpenSymbols') is present."
        )
        else -> {
            // this is a generated else block
        }
    }

    return foundOpenSymbols + openingDelimiter.length to foundClosingSymbols
}

/**
 * Translating the string to the regular expression. For example:
 * "my [special] string{{.*}}should be escaped" -> Regex("my \[special\] string.*should be escaped")
 *
 * @param openingDelimiter opening group of symbols that is used to separate pattern
 * @param closingDelimiter closing group of symbols that is used to separate pattern
 * @param isPartialWarnTextMatch if true - the regex created from expected warning will be wrapped with '.*': .*warn.*
 * @return Regex with an escaped string
 */
fun String.createRegexFromString(
    openingDelimiter: String,
    closingDelimiter: String,
    isPartialWarnTextMatch: Boolean = false
): Regex {
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
    }.joinToString("")

    // if no regex were found in the string we should simply escape all symbols
    val res = if (foundSubStringsWithRegex.isEmpty()) this.escapeSpecialRegexSymbols() else resultWithRegex

    return if (isPartialWarnTextMatch) Regex(".*$res.*") else Regex(res)
}

/**
 * replacing special symbols in the string with the escaped symbol
 */
private fun String.escapeSpecialRegexSymbols() =
        this.replace(Regex("[{}()\\[\\].+*?^$\\\\|]")) { "\\${it.groupValues[0]}" }
