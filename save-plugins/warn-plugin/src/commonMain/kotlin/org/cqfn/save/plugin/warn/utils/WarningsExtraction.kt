/**
 * Methods for warnings extraction from text.
 * TODO: create separate unit tests for these methods.
 */

package org.cqfn.save.plugin.warn.utils

import org.cqfn.save.core.files.readFile
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.plugin.PluginException
import org.cqfn.save.plugin.warn.WarnPluginConfig
import org.cqfn.save.plugin.warn.sarif.adjustToCommonRoot
import org.cqfn.save.plugin.warn.sarif.findAncestorDirContainingFile
import org.cqfn.save.plugin.warn.sarif.toWarnings
import org.cqfn.save.plugin.warn.sarif.topmostTestDirectory

import io.github.detekt.sarif4k.SarifSchema210
import okio.FileSystem
import okio.Path

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.cqfn.save.plugin.warn.adapter.AdapterContext
import org.cqfn.save.plugin.warn.adapter.WarningAdapter
import org.cqfn.save.plugin.warn.adapter.jsonStringToWarnings
import org.cqfn.save.plugin.warn.sarif.SarifWarningAdapter

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
 * @param originalPath
 * @param originalPaths
 * @param fs
 * @param file
 * @return a list of warnings extracted from SARIF file for test [file]
 * @throws PluginException
 */
internal fun collectWarningsFromSarif(
    warnPluginConfig: WarnPluginConfig,
    originalPath: Path,
    originalPaths: List<Path>,
    fs: FileSystem,
    file: Path,
): List<Warning> {
    val sarifFileName = warnPluginConfig.expectedWarningsFileName!!
    val sarif = fs.findAncestorDirContainingFile(originalPath, sarifFileName)?.let { it / sarifFileName }
        ?: throw PluginException(
            "Could not find SARIF file with expected warnings for file $file. " +
                    "Please check if correct `expectedWarningsFormat` is set and if the file is present and called `$sarifFileName`."
        )
    val topmostTestDirectory = fs.topmostTestDirectory(originalPath)
    val sarifWarningAdapter = SarifWarningAdapter()
    return sarifWarningAdapter.jsonStringToWarnings(
        fs.readFile(sarif),
        AdapterContext(topmostTestDirectory, originalPaths.adjustToCommonRoot(topmostTestDirectory))
    )
}
