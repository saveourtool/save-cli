package org.cqfn.save.core.plugin

import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.utils.runIf

import okio.FileSystem
import okio.Path

/**
 * Class that is capable of extracting [ExtraFlags] from a text line
 */
class ExtraFlagsExtractor(private val generalConfig: GeneralConfig,
                          private val fs: FileSystem,
) {
    /**
     * @param path file from which [ExtraFlags] should be extracted
     * @return [ExtraFlags]
     */
    fun extractExtraFlagsFrom(path: Path): ExtraFlags? {
        val allExtraFlagsFromFile = fs.readLines(path)
            .filterAndJoinBy(generalConfig.runConfigPattern!!, '\\')
            .map { extractExtraFlagsFrom(it) }
        return allExtraFlagsFromFile.firstOrNull()
    }

    /**
     * @param line line from which [ExtraFlags] should be extracted
     * @return [ExtraFlags] or null if no match occurred
     */
    internal fun extractExtraFlagsFrom(line: String) = line
        .split(",", ", ")
        .associate { part ->
            val pair = part.split("=", limit = 2).map {
                it.replace("\\=", "=")
            }
            pair.first() to pair.last()
        }
        .let(ExtraFlags::from)
        .also {
            if (it == ExtraFlags("", "")) {
                logDebug("Line <$line> is matched by extraFlagsPattern <${generalConfig.runConfigPattern}>, but no flags have been extracted")
            }
        }
}

/**
 * Filters lines in the receiver list by matching against [regex]. Match results are then extracted (the first matched group
 * is used), and subsequent ones are combined, if they end with `ending`.
 * @see [ExtraFlagsExtractorTest.`should join multiline directives`] for examples.
 *
 * @param regex a regular expression to match lines against; should capture required part of the line in the first capture group
 * @param ending treat lines ending with [ending] as parts of the same directive
 * @return a list of extracted directives
 */
internal fun List<String>.filterAndJoinBy(regex: Regex, ending: Char): List<String> = map(String::trim)
    .mapNotNull { regex.find(it) }
    .map { it.groupValues[1] }
    .runIf({ size > 1 }) {
        // Put a MatchResult into a list, then concat the next one to it, if this one ends with `ending`.
        // Otherwise, append the next one to the list.
        drop(1).fold(mutableListOf(first())) { acc, text ->
            if (acc.last().endsWith(ending)) {
                acc.add(acc.removeLast().trimEnd(ending) + text)
            } else {
                acc.add(text)
            }
            acc
        }
    }

/**
 * Substitute placeholders in `this.execFlags` with values from provided arguments
 *
 * @param execFlags a command that will be executed to check resources and emit warnings
 * @param extraFlags [ExtraFlags] to be inserted into `execFlags`
 * @param fileNames file name or names, that need to be inserted into `execFlags`
 * @return `this.execFlags` with resolved placeholders
 */
fun resolvePlaceholdersFrom(
    execFlags: String?,
    extraFlags: ExtraFlags,
    fileNames: String,
): String {
    requireNotNull(execFlags) {
        "Error: Couldn't find `execFlags`"
    }
    return execFlags
        .replace("\$${ExtraFlags.KEY_ARGS_1}", extraFlags.args1)
        .replace("\$${ExtraFlags.KEY_ARGS_2}", extraFlags.args2).run {
            if (contains("\$fileName")) {
                replace("\$fileName", fileNames)
            } else {
                plus(" $fileNames")
            }
        }
}
