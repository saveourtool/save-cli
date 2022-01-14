package org.cqfn.save.core.utils

import okio.Path
import okio.Path.Companion.toPath
import org.cqfn.save.core.logging.describe
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.plugin.*
import org.cqfn.save.core.result.DebugInfo
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.TestResult

// FixMe: all plugins should use it for executing of command in the future
abstract class ExecCmdBase(
    val execCmd: String,
    val timeOutMillis: Long,
    val testPaths: List<Path>,
    val extraFlagsExtractor: ExtraFlagsExtractor,
    val processBuilder: ProcessBuilder
) {
    fun extractExtraFlags(): ExtraFlags {
        val extraFlagsList = testPaths.mapNotNull { extraFlagsExtractor.extractExtraFlagsFrom(it) }.distinct()
        require(extraFlagsList.size <= 1) {
            "Extra flags for all files in a batch should be same, but you have" +
                    " ${extraFlagsList.size} different sets of flags inside it, namely $extraFlagsList"
        }

        return extraFlagsList.singleOrNull() ?: ExtraFlags("", "")
    }

    fun executeCommandAndGetTestResults(tmpDirName: String, redirectTo: Path?, rootConfigDirectory:): Std {
        // joining test files to string with a batchSeparator if the tested tool supports processing of file batches
        // NOTE: SAVE will pass relative paths of Tests (calculated from testRootConfig dir) into the executed tool
        val fileNamesForExecCmd = when(wildCardInDirectoryMode()) {
            null -> testPaths.joinToString(separator = warnPluginConfig.batchSeparator!!)
            else -> {
                var testRootPath = testPaths[0].parent ?: ".".toPath()
                while (testRootPath.parent != null && testRootPath.parent!!.name != tmpDirName) {
                    testRootPath = testRootPath.parent!!
                }
                "$testRootPath${wildCardInDirectoryMode()}"
            }
        }

        logDebug("Constructed file name for execution for warn plugin: $fileNamesForExecCmd")
        val execFlagsAdjusted = resolvePlaceholdersFrom(execFlags(), extractExtraFlags(), fileNamesForExecCmd)
        val execCmd = "$execCmd $execFlagsAdjusted"
        val time = timeOutMillis.times(testPaths.size)

        val execResult = processBuilder.exec(execCmd, testConfig.getRootConfig().directory.toString(), redirectTo, time)

        val stdout = getStdout(execResult)
        val stderr = execResult.stderr

        return Std(stdout, stderr)
    }

    abstract fun wildCardInDirectoryMode(): String?
    abstract fun execFlags(): String?
    abstract fun getStdout(execResult: ExecutionResult): List<String>
}


class Std(
    out: List<String>,
    err: List<String>
)
