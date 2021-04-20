/**
 * Utilities to run a process and get its result.
 */

package org.cqfn.save.core.utils

import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logWarn

import okio.FileSystem
import okio.Path

/**
 * A class that is capable of executing OS processes and returning their output.
 */
@Suppress("EMPTY_PRIMARY_CONSTRUCTOR")  // expected class should have explicit default constructor
expect class ProcessBuilder() {
    /**
     * Execute [command] and wait for its completion.
     *
     * @param command executable command with arguments
     * @param redirectTo a file where process output should be redirected too. If null, output will be returned as [ExecutionResult.stdout].
     * @return [ExecutionResult] built from process output
     */
    fun exec(command: List<String>, redirectTo: Path?): ExecutionResult
}

/**
 * Class contains common fields for all platforms
 */
class ProcessBuilderInternal {
    /**
     * Typealias
     */
    val fs = FileSystem.SYSTEM

    /**
     * Temporary directory for stderr and stdout (popen can't separate streams, so we do it ourselves)
     */
    val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / this::class.simpleName!!).also {
        fs.createDirectory(it)
    }

    /**
     * Path to stdout file
     */
    val stdoutFile = tmpDir / "stdout.txt".also { logDebug("Created file for stdout of ProcessBuilder in: $tmpDir") }

    /**
     * Path to stderr file
     */
    val stderrFile = tmpDir / "stderr.txt".also { logDebug("Created file for stderr of ProcessBuilder in: $tmpDir") }

    /**
     *  Read data from stdout file, we will use it in [ExecutionResult]
     *
     * @return string containing stdout
     */
    fun getStdout(): List<String> {
        val stdout = fs.read(stdoutFile) {
            generateSequence { readUtf8Line() }.toList()
        }
        return stdout
    }

    /**
     * Read data from stderr file, we will use it in [ExecutionResult]
     *
     * @return string containing stderr
     */
    fun getStderr(): List<String> {
        val stderr = fs.read(stderrFile) {
            generateSequence { readUtf8Line() }.toList()
        }
        return stderr
    }

    /**
     * Modify execution command for popen,
     * stderr will be redirected to tmp file
     *
     * @param command raw command
     * @return command with redirection of stderr to tmp file
     */
    fun prepareCmd(command: List<String>): String {
        val cmd = command.joinToString(" ") + " 2>$stderrFile"
        logDebug("Executing: $cmd")
        return cmd
    }

    /**
     * Finish execution and return depends of status and errors
     *
     * @param stdout output data, will be printed to console or redirected to the file
     * @param status popen exit status
     * @param redirectTo path to the file, where to redirect output
     * @return [ExecutionResult] depends of status and errors
     */
    fun logAndReturn(
        stdout: String,
        status: Int,
        redirectTo: Path?): ExecutionResult {
        if (status == -1) {
            fs.deleteRecursively(tmpDir)
            error("Couldn't close the pipe, exit status: $status")
        }
        val stderr = getStderr()
        fs.deleteRecursively(tmpDir)
        if (stderr.isNotEmpty()) {
            logWarn(stderr.joinToString("\n"))
            return ExecutionResult(status, emptyList(), stderr)
        }
        redirectTo?.let {
            fs.write(redirectTo) {
                write(stdout.encodeToByteArray())
            }
        }
            ?: run {
                logDebug("Execution output:\n$stdout")
            }
        return ExecutionResult(0, stdout.split("\n"), emptyList())
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
