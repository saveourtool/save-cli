/**
 * Utilities to run a process and get its result.
 */

package org.cqfn.save.core.utils

import okio.FileSystem
import okio.Path
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logWarn

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
    // Temporary files for stderr and stdout (popen can't separate streams, so we do it ourselves)
    val fs = FileSystem.SYSTEM
    val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / this::class.simpleName!!).also {
        fs.createDirectory(it)
    }
    val stdoutFile = tmpDir / "stdout.txt"
    val stderrFile = tmpDir / "stderr.txt"

    /**
     *  Read data from stdout file, we will use it in [ExecutionResult]
     */
    fun getStdout(): List<String> {
        val stdout = fs.read(stdoutFile) {
            generateSequence { readUtf8Line() }.toList()
        }
        return stdout
    }

    /**
     * Read data from stderr file, we will use it in [ExecutionResult]
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
     */
    fun prepare(command: List<String>): String {
        logDebug("Created file for stderr: ${stderrFile}")
        val cmd = command.joinToString(" ") + " 2>${stderrFile}"
        logDebug("Executing: $cmd")
        return cmd
    }

    /**
     * Finish execution and return depends of status and errors
     */
    fun logAndReturn(stdout: String, status: Int, redirectTo: Path?): ExecutionResult {
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
        if (redirectTo != null) {
            fs.write(redirectTo) {
                write(stdout.encodeToByteArray())
            }
        } else {
            logDebug("Execution output:\n${stdout}")
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
