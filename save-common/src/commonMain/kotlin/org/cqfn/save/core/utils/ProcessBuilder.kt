/**
 * Utilities to run a process and get its result.
 */

package org.cqfn.save.core.utils

import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logError
import org.cqfn.save.core.logging.logWarn

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

import kotlinx.datetime.Clock

/**
 * A class that is capable of executing processes, specific to different OS and returning their output.
 */
expect class ProcessBuilderInternal(
    stdoutFile: Path,
    stderrFile: Path,
    collectStdout: Boolean) {
    /**
     * Modify execution command according behavior of different OS,
     * also stdout and stderr will be redirected to tmp files
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
 * Class contains common logic for all platforms
 */
@Suppress("INLINE_CLASS_CAN_BE_USED")
class ProcessBuilder {
    /**
     * Singleton that describes the current file system
     */
    private val fs = FileSystem.SYSTEM

    /**
     * Execute [command] and wait for its completion.
     *
     * @param command executable command with arguments
     * @param redirectTo a file where process output should be redirected. If null, output will be returned as [ExecutionResult.stdout].
     * @param collectStdout whether to collect stdout for future usage, if false, [redirectTo] will be ignored
     * @return [ExecutionResult] built from process output
     */
    @Suppress(
        "TOO_LONG_FUNCTION",
        "TooGenericExceptionCaught",
        "ReturnCount")
    fun exec(
        command: String,
        redirectTo: Path?,
        collectStdout: Boolean = true): ExecutionResult {
        if (command.isBlank()) {
            return returnWithError("Command couldn't be empty!")
        }
        // Temporary directory for stderr and stdout (posix `system()` can't separate streams, so we do it ourselves)
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY /
                ("ProcessBuilder_" + Clock.System.now().toEpochMilliseconds()).toPath())
        // Path to stdout file
        val stdoutFile = tmpDir / "stdout.txt"
        // Path to stderr file
        val stderrFile = tmpDir / "stderr.txt"
        // Instance, containing platform-dependent realization of command execution
        val processBuilderInternal = ProcessBuilderInternal(stdoutFile, stderrFile, collectStdout)
        fs.createDirectories(tmpDir)
        fs.createFile(stdoutFile)
        fs.createFile(stderrFile)
        logDebug("Created temp directory $tmpDir for stderr and stdout of ProcessBuilder")
        if (command.contains(">")) {
            logWarn("Found user provided redirections in `$command`. " +
                    "SAVE uses own redirections for internal purpose and will redirect all streams to $tmpDir")
        }
        val commandWithEcho = if (isCurrentOsWindows()) {
            processCommandWithEcho(command)
        } else {
            command
        }
        val cmd = processBuilderInternal.prepareCmd(commandWithEcho)
        logDebug("Executing: $cmd")
        val status = try {
            processBuilderInternal.exec(cmd)
        } catch (ex: Exception) {
            fs.deleteRecursively(tmpDir)
            return returnWithError(ex.message ?: "Couldn't execute $cmd")
        }
        val stdout = fs.readLines(stdoutFile)
        val stderr = fs.readLines(stderrFile)
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

    /**
     * Log error message and return with status = -1
     *
     * @param errMsg error message
     * @return [ExecutionResult] corresponding result with error message and exit code
     */
    private fun returnWithError(errMsg: String): ExecutionResult {
        logError(errMsg)
        return ExecutionResult(-1, emptyList(), listOf(errMsg))
    }

    companion object {
        /**
         * Check whether there are exists `echo` commands, and process them, since in Windows
         * `echo` adds extra whitespaces and newlines. This method will remove them
         *
         * @param command command to process
         * @return unmodified command, if there is no `echo` subcommands, otherwise add parameter `set /p=` to `echo`
         */
        @Suppress("ReturnCount")
        fun processCommandWithEcho(command: String): String {
            if (!command.contains("echo")) {
                return command
            }
            // Command already contains correct signature.
            // We also believe not to met complex cases: `echo a; echo | set /p="a && echo b"`
            val cmdWithoutWhitespaces = command.replace(" ", "")
            if (cmdWithoutWhitespaces.contains("echo|set")) {
                return command
            }
            if (cmdWithoutWhitespaces.contains("echo\"")) {
                logWarn("You can use echo | set /p\"your command\" to avoid extra whitespaces on Windows")
                return command
            }
            // If command is complex (have `&&` or `;`), we need to modify only `echo` subcommands
            val separator = if (command.contains("&&")) {
                "&&"
            } else if (command.contains(";")) {
                ";"
            } else {
                ""
            }
            val listOfCommands = if (separator != "") command.split(separator) as MutableList<String> else mutableListOf(command)
            listOfCommands.forEachIndexed { index, cmd ->
                if (cmd.contains("echo")) {
                    var newEchoCommand = cmd.trim(' ').replace("echo ", " echo | set /p dummyName=\"")
                    // Now we need to add closing `"` in proper place
                    // Despite the fact, that we don't expect user redirections, for out internal tests we use them,
                    // so we need to process such cases
                    // There are three different cases, where we need to insert closing `"`.
                    // 1) Before stdout redirection
                    // 2) Before stderr redirection
                    // 3) At the end of string, if there is no redirections
                    val indexOfStdoutRedirection = if (newEchoCommand.indexOf(">") != -1) newEchoCommand.indexOf(">") else newEchoCommand.length
                    val indexOfStderrRedirection = if (newEchoCommand.indexOf("2>") != -1) newEchoCommand.indexOf("2>") else newEchoCommand.length
                    val insertIndex = minOf(indexOfStdoutRedirection, indexOfStderrRedirection)
                    newEchoCommand = newEchoCommand.substring(0, insertIndex).trimEnd(' ') + "\" " + newEchoCommand.substring(insertIndex, newEchoCommand.length) + " "
                    listOfCommands[index] = newEchoCommand
                }
            }
            val modifiedCommand = listOfCommands.joinToString(separator).trim(' ')
            logDebug("Modify command:`$command` to `$modifiedCommand` because of `echo` on Windows add extra newlines")
            return modifiedCommand
        }
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
