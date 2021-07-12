/**
 * Classes and methods to work with warnings
 */

package org.cqfn.save.plugin.warn.utils

import org.cqfn.save.core.plugin.ResourceFormatException

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
    "ThrowsCount")
internal fun String.extractWarning(warningRegex: Regex,
                                   fileName: String,
                                   line: Int?,
                                   columnGroupIdx: Int?,
                                   messageGroupIdx: Int,
): Warning? {
    val groups = warningRegex.find(this)?.groups ?: return null

    val column = columnGroupIdx?.let {
        try {
            groups[columnGroupIdx]!!.value.toInt()
        } catch (e: Exception) {
            throw ResourceFormatException("Could not extract column number from line [$this], cause: ${e.message}")
        }
    }

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

/**
 * Extract warning from [this] string using provided parameters
 *
 * @param warningRegex regular expression for warning
 * @param columnGroupIdx index of capture group for column number
 * @param lineGroupIdx index of capture group for line number
 * @param messageGroupIdx index of capture group for waring text
 * @param fileNameGroupIdx index of capture group for file name
 * @return a [Warning] or null if [this] string doesn't match [warningRegex]
 * @throws ResourceFormatException when parsing a file
 */
@Suppress(
    "TooGenericExceptionCaught",
    "SwallowedException")
internal fun String.extractWarning(warningRegex: Regex,
                                   fileNameGroupIdx: Int,
                                   lineGroupIdx: Int?,
                                   columnGroupIdx: Int?,
                                   messageGroupIdx: Int,
): Warning? {
    val groups = warningRegex.find(this)?.groups ?: return null

    val fileName = try {
        groups[fileNameGroupIdx]!!.value
    } catch (e: Exception) {
        throw ResourceFormatException("Could not extract file name from line [$this], cause: ${e.message}")
    }

    return extractWarning(false, warningRegex, fileName, lineGroupIdx, columnGroupIdx, messageGroupIdx)
}

/**
 * @param defaultLineMode parameter for default line
 * @param warningRegex regular expression for warning
 * @param fileName file name
 * @param lineGroupIdx index of capture group for line number or line number
 * @param columnGroupIdx index of capture group for column number
 * @param messageGroupIdx index of capture group for waring text
 * @return a [Warning] or null if [this] string doesn't match [warningRegex]
 * @throws ResourceFormatException when parsing a file
 */
@Suppress(
    "TOO_MANY_PARAMETERS",
    "LongParameterList",
    "TooGenericExceptionCaught",
    "SwallowedException")
internal fun String.extractWarning(defaultLineMode: Boolean,
                                   warningRegex: Regex,
                                   fileName: String,
                                   lineGroupIdx: Int?,
                                   columnGroupIdx: Int?,
                                   messageGroupIdx: Int,
): Warning? {
    val groups = warningRegex.find(this)?.groups ?: return null

    val line = if (defaultLineMode) {
        lineGroupIdx
    } else {
        lineGroupIdx?.let {
            try {
                groups[lineGroupIdx]!!.value.toInt()
            } catch (e: Exception) {
                throw ResourceFormatException("Could not extract line number from line [$this], cause: ${e.message}")
            }
        }
    }

    return extractWarning(warningRegex, fileName, line, columnGroupIdx, messageGroupIdx)
}
