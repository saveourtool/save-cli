/**
 * Methods for warnings extraction from text.
 * TODO: create separate unit tests for these methods.
 */

package org.cqfn.save.plugin.warn.utils

import okio.Path
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.plugin.warn.WarnPluginConfig

internal fun collectionMultilineWarnings(
    warnPluginConfig: WarnPluginConfig,
    generalConfig: GeneralConfig,
    linesFile: List<String>,
    file: Path,
): List<Warning> = linesFile.mapIndexed { index, line ->
    val newLineAndMessage = line.getLineNumberAndMessage(
        generalConfig.expectedWarningsPattern!!,
        generalConfig.expectedWarningsEndPattern!!,
        generalConfig.expectedWarningsMiddlePattern!!,
        warnPluginConfig.messageCaptureGroupMiddle!!,
        warnPluginConfig.messageCaptureGroupEnd!!,
        warnPluginConfig.lineCaptureGroup,
        warnPluginConfig.linePlaceholder!!,
        warnPluginConfig.messageCaptureGroup!!,
        index + 1,
        file,
        linesFile,
    )
    with(warnPluginConfig) {
        line.extractWarning(
            generalConfig.expectedWarningsPattern!!,
            file.name,
            newLineAndMessage?.first,
            newLineAndMessage?.second,
            columnCaptureGroup,
            benchmarkMode!!,
        )
    }
}
    .filterNotNull()
    .sortedBy { warn -> warn.message }

internal fun collectionSingleWarnings(
    warnPluginConfig: WarnPluginConfig,
    generalConfig: GeneralConfig,
    linesFile: List<String>,
    file: Path,
): List<Warning> = linesFile.mapIndexed { index, line ->
    val newLine = line.getLineNumber(
        generalConfig.expectedWarningsPattern!!,
        warnPluginConfig.lineCaptureGroup,
        warnPluginConfig.linePlaceholder!!,
        index + 1,
        file,
        linesFile,
    )
    with(warnPluginConfig) {
        line.extractWarning(
            generalConfig.expectedWarningsPattern!!,
            file.name,
            newLine,
            columnCaptureGroup,
            messageCaptureGroup!!,
            benchmarkMode!!,
        )
    }
}
    .filterNotNull()
    .sortedBy { warn -> warn.message }
