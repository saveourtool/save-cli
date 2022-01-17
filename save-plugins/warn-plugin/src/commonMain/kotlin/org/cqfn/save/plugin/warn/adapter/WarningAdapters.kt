package org.cqfn.save.plugin.warn.adapter

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okio.Path
import org.cqfn.save.core.files.fs
import org.cqfn.save.core.files.readLines
import org.cqfn.save.plugin.warn.utils.Warning

inline fun <reified T> WarningAdapter<T>.jsonStringToWarnings(
    rawReport: String,
    ctx: AdapterContext,
): List<Warning> {
    return toWarnings(
        Json.decodeFromString(rawReport),
        ctx,
    )
}

/**
 * Base class for adapters that read a file without any deserialization support.
 * Implementations must implement `toWarning` to convert a line into a Warning.
 * For example, to read warnings from CSV `toWarnings` should contain logic of line splitting and parsing.
 */
abstract class FileFormatWarningAdapter : WarningAdapter<String> {
    fun fileToWarnings(path: Path, ctx: AdapterContext): List<Warning> {
        return fs.readLines(path).flatMap { toWarnings(it, ctx) }
    }
}
