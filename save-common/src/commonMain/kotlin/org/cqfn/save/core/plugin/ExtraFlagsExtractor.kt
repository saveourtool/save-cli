package org.cqfn.save.core.plugin

import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.logging.logWarn

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
        val allExtraFlagsFromFile = fs.readLines(path).mapNotNull {
            extractExtraFlagsFrom(it)
        }
        require(allExtraFlagsFromFile.size <= 1) {
            "Extra flags from multiple comments in a single file are not supported yet, but there are ${allExtraFlagsFromFile.size} in $path"
        }
        return allExtraFlagsFromFile.singleOrNull()
    }

    /**
     * @param line line from which [ExtraFlags] should be extracted
     * @return [ExtraFlags] or null if no match occurred
     */
    @Suppress("COMPACT_OBJECT_INITIALIZATION")  // https://github.com/cqfn/diKTat/issues/1043
    fun extractExtraFlagsFrom(line: String): ExtraFlags? {
        val matchResult = generalConfig.runConfigPattern!!.find(line) ?: return null
        return matchResult.groupValues[1]
            .split(",", ", ")
            .associate {
                val pair = it.split("=", limit = 2).map {
                    it.replace("\\=", "=")
                }
                pair.first() to pair.last()
            }
            .let(ExtraFlags::from)
            .also {
                if (it == ExtraFlags("", "")) {
                    logWarn("Line <$line> is matched by extraFlagsPattern <${generalConfig.runConfigPattern}>, but no flags have been extracted")
                }
            }
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
    fileNames: String): String {
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
