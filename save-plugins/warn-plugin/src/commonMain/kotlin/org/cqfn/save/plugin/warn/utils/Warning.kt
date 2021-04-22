/**
 * Classes and methods to work with warnings
 */

package org.cqfn.save.plugin.warn.utils

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
 */
internal fun String.extractWarning(warningRegex: Regex,
                                   columnGroupIdx: Int?,
                                   lineGroupIdx: Int?,
                                   messageGroupIdx: Int,
): Warning? {
    val groups = warningRegex.find(this)?.groups ?: return null
    val line = lineGroupIdx?.let {
        groups[lineGroupIdx]?.value?.toInt()
    }
        ?: run {
            null
        }
    val column = columnGroupIdx?.let {
        groups[columnGroupIdx]?.value?.toInt()
    }
        ?: run {
            null
        }
    return Warning(
        groups[messageGroupIdx]!!.value,
        line,
        column,
    )
}
