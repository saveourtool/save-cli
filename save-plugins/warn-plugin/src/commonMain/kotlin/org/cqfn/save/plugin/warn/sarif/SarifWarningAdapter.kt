package org.cqfn.save.plugin.warn.sarif

import io.github.detekt.sarif4k.Run
import io.github.detekt.sarif4k.SarifSchema210
import okio.Path
import okio.Path.Companion.toPath
import org.cqfn.save.plugin.warn.utils.Warning

/**
 * Convert this SARIF report to a list of [Warning]s.
 *
 * @param testFiles if this list is not empty, then results from SARIF will be filtered to match paths from [testFiles].
 * [testFiles] should be relative to test root, then URIs from SARIF will be trimmed too and matched against [testFiles].
 */
fun SarifSchema210.toWarnings(testRoot: Path?, testFiles: List<Path>): List<Warning> {
    // "Each run represents a single invocation of a single analysis tool, and the run has to describe the tool that produced it."
    // In case of SAVE this array will probably always have a single element.
    return runs.flatMap {
        it.toWarning(testRoot, testFiles)
    }
}

fun Run.toWarning(testRoot: Path?, testFiles: List<Path>): List<Warning> {
    // "A result is an observation about the code."
    return results?.map { result ->
        // "array of location objects which almost always contains exactly one element"
        // Location is empty for warnings that, e.g., refer to the whole project instead of individual files.
        // Location can have >1 elements, e.g., if the warning suggests a refactoring, that affects multiple files.

        val filePath = result.locations
            ?.singleOrNull()
            ?.physicalLocation
            ?.artifactLocation
            ?.uri
            // assuming that all URIs for SAVE correspond to files
            ?.substringAfter("file://")
            ?.toPath()
            ?.let { if (testRoot != null) it.relativeTo(testRoot) else it }
        result to filePath
    }
        ?.filter { (_, filePath) ->
            testFiles.isEmpty() || filePath in testFiles
        }
        ?.map { (result, filePath) ->
        val (line, column) = result.locations?.map {
            // "The most common case is for a tool to report a physical location, and to specify the location by line and column number."
            it.physicalLocation?.region
        }
            ?.map {
                it?.startLine to it?.startColumn
            }
            ?.singleOrNull()
            ?: (null to null)
        val fileName = filePath?.name ?: ""
        Warning(
            // in the simplest case, Message will only contain `text`
            message = result.message.text ?: "",
            line = line?.toInt(),
            column = column?.toInt(),
            fileName = fileName,
        )
    } ?: emptyList()
}
