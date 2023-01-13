/**
 * Methods for warnings extraction from text.
 * TODO: create separate unit tests for these methods.
 */

package com.saveourtool.save.plugin.warn.utils

import com.saveourtool.save.core.files.readFile
import com.saveourtool.save.core.plugin.GeneralConfig
import com.saveourtool.save.core.plugin.PluginException
import com.saveourtool.save.core.utils.adjustToCommonRoot
import com.saveourtool.save.core.utils.calculatePathToSarifFile
import com.saveourtool.save.core.utils.topmostTestDirectory
import com.saveourtool.save.plugin.warn.WarnPluginConfig
import com.saveourtool.save.plugin.warn.sarif.toWarnings

import io.github.detekt.sarif4k.SarifSchema210
import okio.FileSystem
import okio.Path

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * @param warnPluginConfig
 * @param generalConfig
 * @param linesFile
 * @param file
 * @return a list of warnings extracted from [linesFile]
 */
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

/**
 * @param warnPluginConfig
 * @param generalConfig
 * @param linesFile
 * @param file
 * @return a list of warnings extracted from [linesFile]
 */
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

/**
 * @param warnPluginConfig
 * @param originalPaths
 * @param fs
 * @param workingDirectory initial working directory, when SAVE started
 * @return a list of warnings extracted from SARIF file for test [file]
 * @throws PluginException
 */
internal fun collectWarningsFromSarif(
    warnPluginConfig: WarnPluginConfig,
    originalPaths: List<Path>,
    fs: FileSystem,
    workingDirectory: Path,
): List<Warning> {
    val sarifFileName = warnPluginConfig.expectedWarningsFileName!!

    // Since we have one .sarif file for all tests, just take the first of them as anchor for calculation of paths
    val anchorTestFilePath = originalPaths.first()
    val sarif = calculatePathToSarifFile(
        sarifFileName = sarifFileName,
        anchorTestFilePath = anchorTestFilePath
    )
    val topmostTestDirectory = fs.topmostTestDirectory(anchorTestFilePath)
    return Json.decodeFromString<SarifSchema210>(
        fs.readFile(sarif)
    )
        .toWarnings(topmostTestDirectory, originalPaths.adjustToCommonRoot(topmostTestDirectory), workingDirectory)
}
