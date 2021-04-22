/**
 * Utilities to run a process and get its result.
 */

package org.cqfn.save.core.utils

import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.logging.logDebug

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

import kotlinx.datetime.Clock
import org.cqfn.save.core.files.createFile

/**
 * Typealias
 */
val fs = FileSystem.SYSTEM

/**
 * Temporary directory for stderr and stdout (popen can't separate streams, so we do it ourselves)
 */
val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY /
        ("ProcessBuilder_" + Clock.System.now().toEpochMilliseconds()).toPath())

/**
 * Path to stdout file
 */
val stdoutFile = tmpDir / "stdout.txt"

/**
 * Path to stderr file
 */
val stderrFile = tmpDir / "stderr.txt"

/**
 *  Read data from stdout file, we will use it in [ExecutionResult]
 *
 * @return string containing stdout
 */
fun getStdout() = fs.readLines(stdoutFile)

/**
 * Read data from stderr file, we will use it in [ExecutionResult]
 *
 * @return string containing stderr
 */
fun getStderr() = fs.readLines(stderrFile)

/**
 * Modify execution command for popen,
 * stderr will be redirected to tmp file
 *
 * @param command raw command
 * @return command with redirection of stderr to tmp file
 */
expect fun prepareCmd(command: List<String>): String

/**
 * Finish execution and return depends of status and errors
 *
 * @param stdout output data, will be printed to console or redirected to the file
 * @param status popen exit status
 * @param redirectTo path to the file, where to redirect output
 * @return [ExecutionResult] depends of status and errors
 */
expect fun logAndReturn(status: Int, redirectTo: Path?): ExecutionResult

/**
 * A class that is capable of executing OS processes and returning their output.
 */
@Suppress("EMPTY_PRIMARY_CONSTRUCTOR")  // expected class should have explicit default constructor
expect class ProcessBuilderInternal() {
    /**
     * Execute [command] and wait for its completion.
     *
     * @param command executable command with arguments
     * @return pair of execution exit code and execution output
     */
    fun exec(cmd: String): Int
}

/**
 * Class contains common fields for all platforms
 */
class ProcessBuilder {
    /**
     * Instance, containing platform-dependent realization of command execution
     */
    val processBuilderInternal = ProcessBuilderInternal()

    /**
     * Execute [command] and wait for its completion.
     *
     * @param command executable command with arguments
     * @param redirectTo a file where process output should be redirected too. If null, output will be returned as [ExecutionResult.stdout].
     * @return [ExecutionResult] built from process output
     */
    fun exec(command: List<String>, redirectTo: Path?): ExecutionResult {
        fs.createDirectories(tmpDir)
        fs.createFile(stdoutFile)
        logDebug("Created file for stdout of ProcessBuilder in: $tmpDir")
        fs.createFile(stderrFile)
        logDebug("Created file for stderr of ProcessBuilder in: $tmpDir")
        val cmd = prepareCmd(command)
        val status = processBuilderInternal.exec(cmd)
        return logAndReturn(status, redirectTo)
    }
}

/**
 * @property code exit code
 * @property stdout content of stdout
 * @property stderr content of stderr
 */
data class ExecutionResult(
    val code: Int,
    val stdout: List<String>,
    val stderr: List<String>,
)
