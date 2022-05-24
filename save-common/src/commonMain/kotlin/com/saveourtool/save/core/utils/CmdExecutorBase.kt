package com.saveourtool.save.core.utils

import com.saveourtool.save.core.config.TestConfig
import com.saveourtool.save.core.logging.logTrace
import com.saveourtool.save.core.plugin.ExtraFlags
import com.saveourtool.save.core.plugin.ExtraFlagsExtractor
import com.saveourtool.save.core.plugin.resolvePlaceholdersFrom

import okio.Path
import okio.Path.Companion.toPath

/**
 * FixMe: All plugins should use it for executing of commands in the future
 *
 * @property execCmd - execution command. Will be modified with extra flags and test files
 */
abstract class CmdExecutorBase(
    val execCmd: String,
    private val timeOutMillis: Long,
    private val copyPaths: List<Path>,
    private val extraFlagsExtractor: ExtraFlagsExtractor,
    private val pb: ProcessBuilder,
    private val testConfig: TestConfig,
) {
    private lateinit var constructedExecCmd: String

    /**
     * Simple method for extracting of extra flags, that will be used later in execCmd
     */
    private fun extractExtraFlags(): ExtraFlags {
        val extraFlagsList = copyPaths.mapNotNull { extraFlagsExtractor.extractExtraFlagsFrom(it) }.distinct()
        require(extraFlagsList.size <= 1) {
            "Extra flags for all files in a batch should be same, but you have" +
                    " ${extraFlagsList.size} different sets of flags inside it, namely $extraFlagsList"
        }

        return extraFlagsList.singleOrNull() ?: ExtraFlags("", "")
    }

    /**
     * @param redirectTo a file where process output and errors should be redirected.
     * If null, output will be returned as [ExecutionResult.stdout] and [ExecutionResult.stderr].
     * @return the result after the execution of [constructedExecCmd]
     */
    fun execCmdAndGetExecutionResults(redirectTo: Path?): ExecutionResult {
        val time = timeOutMillis.times(copyPaths.size)
        val execResult = pb.exec(constructedExecCmd, testConfig.getRootConfig().directory.toString(), redirectTo, time)

        return ExecutionResult(execResult.code, execResult.getStdout(), execResult.stderr)
    }

    /**
     * Method does the construction of execution cmd and assigns it to [constructedExecCmd]
     * In fact, it does the following: execCmd + execFlags + fileNames + extraFlags
     *
     * @param tmpDirName - the name of test file in the temporary directory
     * @return constructed command for the execution. At the same time updates [constructedExecCmd]
     */
    fun constructExecCmd(tmpDirName: String): String = "$execCmd ${
        resolvePlaceholdersFrom(
            getExecFlags(),
            extractExtraFlags(),
            constructFileNamesForExecCmd(tmpDirName)
        )
    }"
        .also {
            constructedExecCmd = it
        }

    private fun constructFileNamesForExecCmd(tmpDirName: String): String {
        // joining test files to string with a batchSeparator if the tested tool supports processing of file batches
        // NOTE: SAVE will pass relative paths of Tests (calculated from testRootConfig dir) into the executed tool
        val fileNamesForExecCmd = getWildCardInDirectoryMode()?.let {
            var testRootPath = copyPaths[0].parent ?: ".".toPath()
            while (testRootPath.parent != null && testRootPath.parent!!.name != tmpDirName) {
                testRootPath = testRootPath.parent!!
            }
            "$testRootPath${getWildCardInDirectoryMode()}"
        } ?: copyPaths.joinToString(separator = getBatchSeparator())

        logTrace("Constructed file names for execution for warn plugin: $fileNamesForExecCmd")

        return fileNamesForExecCmd
    }

    /**
     * This method should return a wildCard if the tool is run on the directory in the directory mode.
     *
     * @return a string with a wildcard OR null if the tool is not run in the directory mode
     */
    abstract fun getWildCardInDirectoryMode(): String?

    /**
     * @return string with extra flags that are used to construct exec cmd (flags that are added to your tool console)
     */
    abstract fun getExecFlags(): String?

    /**
     * @return stdout from the execution result. Can be simply taken from ExecutionResult.stdout or modified
     */
    abstract fun ExecutionResult.getStdout(): List<String>

    /**
     * @return separator for test names that are passed to the tool in case when batch mode is used. For example:
     * if barchSeparator = ";" -> tool file1FromBatch,file2FromBatch
     */
    abstract fun getBatchSeparator(): String
}
