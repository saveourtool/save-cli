package org.cqfn.save.plugin.warn.utils


data class Warning(
    val message: String,
    val line: Int?,
    val column: Int?,
)

internal fun String.extractWarning(warningRegex: Regex,
                                   columnGroupIdx: Int?,
                                   lineGroupIdx: Int?,
                                   messageGroupIdx: Int,
): Warning? {
    val groups = warningRegex.find(this)?.groups ?: return null
    val line = if (lineGroupIdx != null) groups[lineGroupIdx]?.value?.toInt() else null
    val column =  if (columnGroupIdx != null) groups[columnGroupIdx]?.value?.toInt() else null
    return Warning(
        groups[messageGroupIdx]!!.value,
        line,
        column,
    )
}