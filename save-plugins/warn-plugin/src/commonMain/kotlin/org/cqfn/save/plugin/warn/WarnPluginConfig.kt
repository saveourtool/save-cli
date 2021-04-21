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
    // todo: add warnings input pattern
    val warningsOutputPattern: Regex,
    val warningTextHasLine: Boolean = true,
    val warningTextHasColumn: Boolean = true,
    val lineCaptureGroup: Int?,
    val columnCaptureGroup: Int?,
    val messageCaptureGroup: Int,
    val testResources: List<Path> = emptyList(),
) : PluginConfig {
    init {
        require(warningTextHasLine xor (lineCaptureGroup == null)) {
            "warn-plugin configuration error: either warningTextHasLine should be false (actual: $warningTextHasLine) or lineCaptureGroup should be provided (actual: $lineCaptureGroup}"
        }
        require(warningTextHasColumn xor (columnCaptureGroup == null)) {
            "warn-plugin configuration error: either warningTextHasColumn should be false (actual: $warningTextHasColumn) or columnCaptureGroup should be provided (actual: $columnCaptureGroup}"
        }
    }
}

internal fun warningRegex(warnPluginConfig: WarnPluginConfig): Regex = StringBuilder(";warn:").run {
    if (warnPluginConfig.warningTextHasColumn) append("(\\d+):")
    if (warnPluginConfig.warningTextHasLine) append("(\\d+):")
    append(" (.+)")
    Regex(toString())
}
