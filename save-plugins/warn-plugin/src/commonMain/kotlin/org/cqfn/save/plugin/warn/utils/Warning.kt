/**
 * Classes and methods to work with warnings
 */

package org.cqfn.save.plugin.warn.utils

import org.cqfn.save.core.logging.describe
import org.cqfn.save.core.logging.logWarn
import org.cqfn.save.core.plugin.ResourceFormatException

import okio.Path

/**
 * Class for warnings which should be discovered and compared wit analyzer output
 *
 * @property message warning text
 * @property line line on which this warning occurred
 * @property column column in which this warning occurred
 * @property fileName file name
 */
data class Warning(
    val message: String,
    val line: Int?,
    val column: Int?,
    val fileName: String,
)

/**
 * @param warningRegex regular expression for warning
 * @param lineGroupIdx index of capture group for line number
 * @param placeholder placeholder for line
 * @param lineNum number of line
 * @param file path to test file
 * @param linesFile lines of file
 * @return a [Warning] or null if [this] string doesn't match [warningRegex]
 * @throws ResourceFormatException when parsing a file
 */
@Suppress(
    "TooGenericExceptionCaught",
    "LongParameterList",
    "NestedBlockDepth",
    "ReturnCount",
    // fixme: add `cause` parameter to `PluginException`
    "SwallowedException",
    "TOO_MANY_PARAMETERS",
    "AVOID_NULL_CHECKS",
)
fun String.getLineNumber(warningRegex: Regex,
                         lineGroupIdx: Long?,
                         placeholder: String,
                         lineNum: Int?,
                         file: Path?,
                         linesFile: List<String>?,
): Int? {
    if (lineGroupIdx == null) {
        // line capture group is not configured in save.toml
        return null
    }

    val groups = warningRegex.find(this)?.groups ?: return null
    val lineValue = groups[lineGroupIdx.toInt()]!!.value
    return if (lineValue.isEmpty() && lineNum != null && linesFile != null) {
        nextLineNotMatchingRegex(file!!, warningRegex, linesFile, lineNum)
    } else {
        lineValue.toIntOrNull() ?: run {
            if (lineValue[0] != placeholder[0]) {
                throw ResourceFormatException("The group <$lineValue> is neither a number nor a placeholder.")
            }
            try {
                val adjustment = lineValue.substringAfterLast(placeholder)
                lineNum!! + adjustment.ifBlank { "0" }.toInt()
            } catch (e: Exception) {
                throw ResourceFormatException("Could not extract line number from line [$this], cause: ${e.describe()}")
            }
        }
    }
}

/**
 * Extract warning from [this] string using provided parameters
 *
 * @param warningRegex regular expression for warning
 * @param columnGroupIdx index of capture group for column number
 * @param messageGroupIdx index of capture group for waring text
 * @param fileName file name
 * @param line line number of warning
 * @return a [Warning] or null if [this] string doesn't match [warningRegex]
 * @throws ResourceFormatException when parsing a file
 */
internal fun String.extractWarning(warningRegex: Regex,
                                   fileName: String,
                                   line: Int?,
                                   columnGroupIdx: Long?,
                                   messageGroupIdx: Long,
): Warning? {
    val groups = warningRegex.find(this)?.groups ?: return null

    val column = getRegexGroupSafe(columnGroupIdx, groups, this, "column number")?.toIntOrNull()
    val message = getRegexGroupSafe(messageGroupIdx, groups, this, "warning message")!!
    return Warning(
        message,
        line,
        column,
        fileName,
    )
}

/**
 * Extract warning from [this] string using provided parameters
 *
 * @param warningRegex regular expression for warning
 * @param columnGroupIdx index of capture group for column number
 * @param messageGroupIdx index of capture group for waring text
 * @param fileNameGroupIdx index of capture group for file name
 * @param line line number of warning
 * @return a [Warning] or null if [this] string doesn't match [warningRegex]
 * @throws ResourceFormatException when parsing a file
 */
@Suppress(
    "TooGenericExceptionCaught",
    "SwallowedException")
internal fun String.extractWarning(warningRegex: Regex,
                                   fileNameGroupIdx: Long,
                                   line: Int?,
                                   columnGroupIdx: Long?,
                                   messageGroupIdx: Long,
): Warning? {
    val groups = warningRegex.find(this)?.groups ?: return null
    val fileName = getRegexGroupSafe(fileNameGroupIdx, groups, this, "file name")!!

    return extractWarning(warningRegex, fileName, line, columnGroupIdx, messageGroupIdx)
}

/**
 * @param warningRegex regular expression for warning
 * @param lineGroupIdx index of capture group for line number
 * @return line number
 */
internal fun String.getLineNumber(warningRegex: Regex,
                                  lineGroupIdx: Long?,
): Int? {
    val groups = warningRegex.find(this)?.groups ?: return null
    return getRegexGroupSafe(lineGroupIdx, groups, this, "file name")?.toInt()
}

@Suppress(
    "WRONG_NEWLINES",
    "TooGenericExceptionCaught",
    "SwallowedException",
)
private fun getRegexGroupSafe(idx: Long?,
                              groups: MatchGroupCollection,
                              line: String,
                              exceptionMessage: String,
): String? {
    return idx?.let {
        try {
            return groups[idx.toInt()]!!.value
        } catch (e: Exception) {
            throw ResourceFormatException("Could not extract $exceptionMessage from line [$line], cause: ${e.message}")
        }
    }
}

/**
 * Returns number of the next line after [lineNum] that doesn't match [regex].
 */
private fun nextLineNotMatchingRegex(
    file: Path,
    regex: Regex,
    linesFile: List<String>,
    lineNum: Int
): Int {
    val fileSize = linesFile.size
    // next line without warn comment
    val nextLineNumber = lineNum + 1 + linesFile.drop(lineNum).takeWhile { regex.containsMatchIn(it) }.count()
    return if (nextLineNumber > fileSize) {
        logWarn("Some warnings are at the end of the file: <$file>. They will be assigned the following line: $nextLineNumber")
        fileSize
    } else {
        nextLineNumber
    }
}
