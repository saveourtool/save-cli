/**
 * Classes and methods to work with warnings
 */

package org.cqfn.save.plugin.warn.utils

import org.cqfn.save.plugin.warn.ResourceFormatException

/**
 * Class for warnings which should be discovered and compared wit analyzer output
 *
 * @property message warning text
 * @property line line on which this warning occurred
 * @property column column in which this warning occurred
 */
data class Warning(
    val message: String,
    val line: Int?,
    val column: Int?,
)

/**
 * Extract warning from [this] string using provided parameters
 *
 * @param warningRegex regular expression for warning
 * @param columnGroupIdx index of capture group for column number
 * @param lineGroupIdx index of capture group for line number
 * @param messageGroupIdx index of capture group for waring text
 * @return a [Warning] or null if [this] string doesn't match [warningRegex]
 * @throws ResourceFormatException when parsing a file
 */
internal fun String.extractWarning(warningRegex: Regex,
                                   columnGroupIdx: Int?,
                                   lineGroupIdx: Int?,
                                   messageGroupIdx: Int,
): Warning? {
    val groups = warningRegex.find(this)?.groups ?: return null

    val line = lineGroupIdx?.let {
        try {
            groups[lineGroupIdx]!!.value.toInt()
        } catch (e: Exception) {
            throw ResourceFormatException("Invalid line format in test file: $this")
        }
    }

    val column = columnGroupIdx?.let {
        try {
            groups[columnGroupIdx]!!.value.toInt()
        } catch (e: Exception) {
            throw ResourceFormatException("Invalid column format in test file: $this")
        }
    }

    val message = try {
        groups[messageGroupIdx]!!.value
    } catch (e: Exception) {
        throw ResourceFormatException("Invalid warning message in test file: $this")
    }
    return Warning(
        message,
        line,
        column,
    )
}
