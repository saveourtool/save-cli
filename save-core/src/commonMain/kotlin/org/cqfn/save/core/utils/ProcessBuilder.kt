/**
 * Utilities to run a process and get its result.
 */

package org.cqfn.save.core.utils

import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logWarn

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

import kotlinx.datetime.Clock

/**
 * Typealias
 */
val fs = FileSystem.SYSTEM

/**
 * Temporary directory for stderr and stdout (posix `system()` can't separate streams, so we do it ourselves)
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
 * A class that is capable of executing OS processes and returning their output.
 */
@Suppress("EMPTY_PRIMARY_CONSTRUCTOR")  // expected class should have explicit default constructor
expect class ProcessBuilderInternal() {
    /**
     * Modify execution command for popen,
     * stdout and stderr will be redirected to tmp files
     *
     * @param command raw command
     * @return command with redirection of stderr to tmp file
     */
    fun prepareCmd(command: String): String

    /**
     * Execute [cmd] and wait for its completion.
     *
     * @param cmd executable command with arguments
     * @return exit status
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
        fs.createFile(stderrFile)
        logDebug("Created temp directory $tmpDir for stderr and srdout of ProcessBuilder")
        val userCmd = command.joinToString(" ")
        if (userCmd.contains(">")) {
            logWarn("Found user provided redirections in `$userCmd`. " +
                    "SAVE use own redirections for internal purpose and will redirect it to the $tmpDir")
        }
        val cmd = processBuilderInternal.prepareCmd(userCmd)
        val status = processBuilderInternal.exec(cmd, redirectTo)
        val stdout = getStdout()
        val stderr = getStderr()
        fs.deleteRecursively(tmpDir)
        logDebug("Removed temp directory $tmpDir")
        if (stderr.isNotEmpty()) {
            logWarn(stderr.joinToString("\n"))
        }
        redirectTo?.let {
            fs.write(redirectTo) {
                write(stdout.joinToString("\n").encodeToByteArray())
            }
        } ?: logDebug("Execution output:\n$stdout")
        return ExecutionResult(status, stdout, stderr)
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
