package org.cqfn.save.plugin.warn.utils

import io.github.detekt.sarif4k.Run
import io.github.detekt.sarif4k.SarifSchema210

fun SarifSchema210.toWarnings(): List<Warning> {
    // "Each run represents a single invocation of a single analysis tool, and the run has to describe the tool that produced it."
    // In case of SAVE this array will probably always have a single element.
    return runs.flatMap {
        it.toWarning()
    }
}

fun Run.toWarning(): List<Warning> {
    // "A result is an observation about the code."
    return results?.map { result ->
        // "array of location objects which almost always contains exactly one element"
        // Location is empty for warnings that, e.g., refer to the whole project instead of individual files.
        // Location can have >1 elements, e.g., if the warning suggests a refactoring, that affects multiple files.
        val (line, column) = result.locations?.map {
            // "The most common case is for a tool to report a physical location, and to specify the location by line and column number."
            it.physicalLocation?.region
        }
            ?.map {
                it?.startLine to it?.startColumn
            }
            ?.singleOrNull()
            ?: (null to null)
        Warning(
            // in the simplest case, Message will only contain `text`
            message = result.message.text ?: "",
            line = line?.toInt(),
            column = column?.toInt(),
            // todo: convert it to filename only?
            fileName = result.locations?.singleOrNull()?.physicalLocation?.artifactLocation?.uri ?: "",
        )
    } ?: emptyList()
}
