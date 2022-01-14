package org.cqfn.save.core.utils

import okio.Path
import okio.Path.Companion.toPath
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.plugin.*

// FixMe: all plugins should use it for executing of command in the future
abstract class CmdExecutorBase(
    val execCmd: String,
    private val timeOutMillis: Long,
    private val copyPaths: List<Path>,
    private val extraFlagsExtractor: ExtraFlagsExtractor,
    private val pb: ProcessBuilder,
    private val testConfig: TestConfig,
) {
    lateinit var constructedExecCmd: String

    private fun extractExtraFlags(): ExtraFlags {
        val extraFlagsList = copyPaths.mapNotNull { extraFlagsExtractor.extractExtraFlagsFrom(it) }.distinct()
        require(extraFlagsList.size <= 1) {
            "Extra flags for all files in a batch should be same, but you have" +
                    " ${extraFlagsList.size} different sets of flags inside it, namely $extraFlagsList"
        }

        return extraFlagsList.singleOrNull() ?: ExtraFlags("", "")
    }

    fun executeCommandAndGetTestResults(redirectTo: Path?): Std {
        val time = timeOutMillis.times(copyPaths.size)
        val execResult = pb.exec(constructedExecCmd, testConfig.getRootConfig().directory.toString(), redirectTo, time)

        return Std(getStdout(execResult), execResult.stderr)
    }

    fun constructExecCmd(tmpDirName: String): String {
        val execFlagsAdjusted = resolvePlaceholdersFrom(execFlags(), extractExtraFlags(), constructFileNamesForExecCmd(tmpDirName))
        constructedExecCmd = "$execCmd $execFlagsAdjusted"
        return constructedExecCmd
    }

    private fun constructFileNamesForExecCmd(tmpDirName: String): String {
        // joining test files to string with a batchSeparator if the tested tool supports processing of file batches
        // NOTE: SAVE will pass relative paths of Tests (calculated from testRootConfig dir) into the executed tool
        val fileNamesForExecCmd = when(wildCardInDirectoryMode()) {
            null -> copyPaths.joinToString(separator = batchSeparator())
            else -> {
                var testRootPath = copyPaths[0].parent ?: ".".toPath()
                while (testRootPath.parent != null && testRootPath.parent!!.name != tmpDirName) {
                    testRootPath = testRootPath.parent!!
                }
                "$testRootPath${wildCardInDirectoryMode()}"
            }
        }

        logDebug("Constructed file name for execution for warn plugin: $fileNamesForExecCmd")

        return fileNamesForExecCmd
    }

    abstract fun wildCardInDirectoryMode(): String?
    abstract fun execFlags(): String?
    abstract fun getStdout(execResult: ExecutionResult): List<String>
    abstract fun batchSeparator(): String
}

class Std(
    val out: List<String>,
    val err: List<String>,
)
