@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package org.cqfn.save.plugin.warn.adapter

import org.cqfn.save.core.files.fs
import org.cqfn.save.core.files.readLines
import org.cqfn.save.plugin.warn.utils.Warning

import okio.Path

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Base class for adapters that read a file without any deserialization support.
 * Implementations must implement `toWarning` to convert a line into a Warning.
 * For example, to read warnings from CSV `toWarnings` should contain logic of line splitting and parsing.
 */
@Suppress("CLASS_SHOULD_NOT_BE_ABSTRACT")  // https://github.com/analysis-dev/diktat/issues/1173
abstract class FileFormatWarningAdapter : WarningAdapter<String> {
    /**
     * @param path a file that contains warnings
     * @param ctx context to use additional data in conversion process
     * @return a list of warnings
     */
    fun fileToWarnings(path: Path, ctx: AdapterContext): List<Warning> = fs.readLines(path).flatMap { toWarnings(it, ctx) }
}

/**
 * @param rawReport a string containing JSON-encoded report of type [T]
 * @param ctx
 * @return a list of warnings
 */
inline fun <reified T> WarningAdapter<T>.jsonStringToWarnings(
    rawReport: String,
    ctx: AdapterContext,
): List<Warning> = toWarnings(
    Json.decodeFromString(rawReport),
    ctx,
)
