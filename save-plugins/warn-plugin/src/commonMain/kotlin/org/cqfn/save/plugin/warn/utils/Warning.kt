/**
 * Classes and methods to work with warnings
 */

package org.cqfn.save.plugin.warn.utils

import org.cqfn.save.core.logging.describe
import org.cqfn.save.core.logging.logWarn
import org.cqfn.save.core.plugin.ResourceFormatException

import okio.Path

private val defaultLinePattern = Regex("// ;warn: (.*)")

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
@Suppress(
    "TooGenericExceptionCaught",
    "SwallowedException",
    "ThrowsCount",
    "TOO_LONG_FUNCTION",
    "ReturnCount",
    "ComplexMethod",
    "NestedBlockDepth",
    "AVOID_NULL_CHECKS",
)
internal fun String.extractWarning(warningRegex: Regex,
                                   fileName: String,
                                   line: Int?,
                                   columnGroupIdx: Int?,
                                   messageGroupIdx: Int,
): Warning? {
    val groups = warningRegex.find(this)?.groups
    val defaultLineGroups = if (groups == null) {
        defaultLinePattern.find(this)?.groups
    } else {
        null
    }

    if (groups == null && defaultLineGroups == null) {
        return null
    }

    groups?.let {
        val column = getRegexGroupSafe(columnGroupIdx, groups, this)
        val message = try {
            groups[messageGroupIdx]!!.value
        } catch (e: Exception) {
            throw ResourceFormatException("Could not extract warning message from line [$this], cause: ${e.message}")
        }
        return Warning(
            message,
            line,
            column,
            fileName,
        )
    }
        ?: defaultLineGroups.let {
            val message = try {
                defaultLineGroups?.get(1)?.value
            } catch (e: Exception) {
                throw ResourceFormatException("Could not extract warning message from line [$this], cause: ${e.message}")
            }
            return message?.let {
                Warning(
                    it,
                    line,
                    null,
                    fileName,
                )
            }
        }
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
                                   fileNameGroupIdx: Int,
                                   line: Int?,
                                   columnGroupIdx: Int?,
                                   messageGroupIdx: Int,
): Warning? {
    val groups = warningRegex.find(this)?.groups ?: return null

    val fileName = try {
        groups[fileNameGroupIdx]!!.value
    } catch (e: Exception) {
        throw ResourceFormatException("Could not extract file name from line [$this], cause: ${e.message}")
    }

    return extractWarning(warningRegex, fileName, line, columnGroupIdx, messageGroupIdx)
}

/**
 * @param warningRegex regular expression for warning
 * @param lineGroupIdx index of capture group for line number
 * @param placeholder placeholder for line
 * @param lineNum number of line
 * @param file
 * @param linesFile
 * @return a [Warning] or null if [this] string doesn't match [warningRegex]
 * @throws ResourceFormatException when parsing a file
 */
@Suppress(
    "TooGenericExceptionCaught",
    "SwallowedException",
    "NestedBlockDepth",
    "LongParameterList",
    "ReturnCount",
    "ComplexMethod",
    "TOO_LONG_FUNCTION",
    "TOO_MANY_PARAMETERS",
    "AVOID_NULL_CHECKS",
)
internal fun String.getLineNumber(warningRegex: Regex,
                                  lineGroupIdx: Int?,
                                  placeholder: String,
                                  lineNum: Int?,
                                  file: Path?,
                                  linesFile: List<String>?,
): Int? {
    val groups = warningRegex.find(this)?.groups
    val defaultLineGroups = if (groups == null) {
        defaultLinePattern.find(this)?.groups
    } else {
        null
    }

    if (groups == null && defaultLineGroups == null) {
        return null
    }

    groups?.let {
        return lineGroupIdx?.let {
            val lineValue = groups[lineGroupIdx]!!.value
            if (lineValue.isEmpty()) {
                return plusLine(file, warningRegex, linesFile, lineNum)
            } else {
                lineValue.toIntOrNull() ?: run {
                    val lineGroup = groups[lineGroupIdx]!!.value
                    if (lineGroup[0] != placeholder[0]) {
                        throw ResourceFormatException("The group <$lineGroup> is neither a number nor a placeholder.")
                    }
                    try {
                        val line = lineGroup.substringAfterLast(placeholder)
                        lineNum!! + 1 + if (line.isNotEmpty()) line.toInt() else 0
                    } catch (e: Exception) {
                        throw ResourceFormatException("Could not extract line number from line [$this], cause: ${e.describe()}")
                    }
                }
            }
        }
    }
        ?: defaultLineGroups.let {
            return plusLine(file, warningRegex, linesFile, lineNum)
        }
}

private fun getRegexGroupSafe(idx: Int?,
                              groups: MatchGroupCollection,
                              line: String,
) = idx?.let {
    try {
        val colValue = groups[idx]!!.value
        if (colValue.isEmpty()) {
            null
        } else {
            colValue.toInt()
        }
    } catch (e: Exception) {
        throw ResourceFormatException("Could not extract column number from line [$line], cause: ${e.message}")
    }
}

private fun plusLine(
    file: Path?,
    warningRegex: Regex,
    linesFile: List<String>?,
    lineNum: Int?
): Int {
    var x = 1
    val fileSize = linesFile!!.size
    while (lineNum!! - 1 + x < fileSize && (warningRegex.find(linesFile[lineNum - 1 + x]) != null || defaultLinePattern.find(linesFile[lineNum - 1 + x]) != null)) {
        x++
    }
    val newLine = lineNum + x
    if (newLine > fileSize) {
        logWarn("Some warnings are at the end of the file: <$file>. They will be assigned the following line: $newLine")
        return fileSize
    }
    return newLine
}