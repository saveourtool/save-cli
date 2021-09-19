package org.cqfn.save.plugin.warn.utils

private val DELIMITER_NOT_FOUND = Pair(-1, -1)

/**
 * Finding indexes of all delimited groups.
 * For example, if openingDelimiter = ( , closingDelimiter = ) , this = aaa(bbb)ccc(eee) =>
 * method will return indexes of the delimited string (start inclusive, end - exclusive) - bbb - map(4 = 6, 12 = 14), counted from 0.
 *
 * @param openingDelimiter opening group of symbols that is used to separate pattern
 * @param closingDelimiter closing group of symbols that is used to separate pattern
 */
fun String.findDelimitedSubStringsWith(openingDelimiter: String, closingDelimiter: String): MutableMap<Int, Int> {
    // finding first delimited group in the initial string
    var delimitedGroup = this.findFirstDelimitedSubStringBy(openingDelimiter, closingDelimiter)
    var nextPartOfString = this
    val result = if (delimitedGroup != DELIMITER_NOT_FOUND) mutableMapOf(delimitedGroup) else mutableMapOf()
    while (true) {
        // offset that is used to restore indexes in the initial string (as the logic below is applied to substring)
        val offset = delimitedGroup.second + closingDelimiter.length
        // substring without a part of a string that was already processed (shifted by offset)
        nextPartOfString = nextPartOfString.substring(offset, nextPartOfString.length)
        delimitedGroup = nextPartOfString.findFirstDelimitedSubStringBy(openingDelimiter, closingDelimiter)
        // no need to process if there are no delimiters in the substring
        if (delimitedGroup == DELIMITER_NOT_FOUND) {
            break
        }
        result[offset + delimitedGroup.first] = offset + delimitedGroup.second
    }

    return result
}

/**
 * Finding the first group of strings that is delimited by openingDelimiter and closingDelimiter.
 * For example, if openingDelimiter = ( , closingDelimiter = ) , this = aaa(bbb)ccc(eee) =>
 * method will return indexes delimited string (start inclusive, end - exclusive) - bbb - Pair(4, 6), counted from 0.
 *
 * Returns DELIMITER_NOT_FOUND - Pair(-1, -1) if no opening/closing group found
 *
 * @param openingDelimiter opening group of symbols that is used to separate pattern
 * @param closingDelimiter closing group of symbols that is used to separate pattern
 * @throws IllegalArgumentException in case of invalid string and invalid delimiters
 */
fun String.findFirstDelimitedSubStringBy(openingDelimiter: String, closingDelimiter: String): Pair<Int, Int> {
    val foundOpenSymbols = this.indexOf(openingDelimiter)
    val foundClosingSymbols = this.indexOf(closingDelimiter)

    when {
        // not found any regular expressions in the warning
        foundOpenSymbols == -1 && foundClosingSymbols == -1 -> return DELIMITER_NOT_FOUND
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
    }

    return foundOpenSymbols + openingDelimiter.length  to foundClosingSymbols
}
