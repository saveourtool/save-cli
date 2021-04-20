package org.cqfn.save.plugin.warn

import okio.Path
import org.cqfn.save.core.plugin.PluginConfig

/**
 * @property execCmd a command that will be executed to check resources and emit warnings
 * @property warningsOutputPattern a regular expression by which warnings will be discovered in the process output
 * @property warningTextHasLine whether line number is included in [warningsOutputPattern]
 * @property warningTextHasColumn whether column number is included in [warningsOutputPattern]
 */
data class WarnPluginConfig(
    val execCmd: String,
    val warningsOutputPattern: Regex,
    val warningTextHasLine: Boolean,
    val warningTextHasColumn: Boolean,
    val testResources: List<Path> = emptyList(),
) : PluginConfig {
}

internal fun warningRegex(warnPluginConfig: WarnPluginConfig): Regex = StringBuilder(";warn:").run {
    if (warnPluginConfig.warningTextHasColumn) append("(\\d+):")
    if (warnPluginConfig.warningTextHasLine) append("(\\d+):")
    append(" (.+)")
    Regex(toString())
}
