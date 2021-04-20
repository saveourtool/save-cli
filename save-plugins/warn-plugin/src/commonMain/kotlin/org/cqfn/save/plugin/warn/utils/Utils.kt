package org.cqfn.save.plugin.warn.utils


data class Warning(
    val message: String,
    val line: Int?,
    val column: Int?,
)

internal fun String.extractWarning(warningRegex: Regex, hasColumn: Boolean, hasLine: Boolean): Warning? {
    val groups = warningRegex.matchEntire(this)?.groups ?: return null
    return Warning(
        groups.last()!!.value,
        if (hasLine) groups[1]?.value?.toInt() else null,
        if (hasLine && hasColumn) groups[2]?.value?.toInt() else if (hasColumn) groups[1]?.value?.toInt() else null,
    )
}