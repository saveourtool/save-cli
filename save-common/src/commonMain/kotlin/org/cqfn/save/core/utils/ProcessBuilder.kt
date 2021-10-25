/**
 * Utilities to run a process and get its result.
 */

package org.cqfn.save.core.utils

import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logError
import org.cqfn.save.core.logging.logTrace
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
    useInternalRedirections: Boolean) {
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
 *
 * @property useInternalRedirections whether to collect output for future usage, if false, [redirectTo] will be ignored
 * @property fs describes the current file system
 */
class ProcessBuilder(private val useInternalRedirections: Boolean, private val fs: FileSystem) {
    /**
     * Execute [command] and wait for its completion.
     *
     * @param command executable command with arguments
     * @param directory where to execute provided command, i.e. `cd [directory]` will be performed before [command] execution
     * @param redirectTo a file where process output and errors should be redirected. If null, output will be returned as [ExecutionResult.stdout] and [ExecutionResult.stderr].
     * @return [ExecutionResult] built from process output
     * @throws ProcessExecutionException in case of impossibility of command execution
     */
    @Suppress(
        "TOO_LONG_FUNCTION",
        "TooGenericExceptionCaught",
        "ReturnCount")
    fun exec(
        command: String,
        directory: String,
        redirectTo: Path?): ExecutionResult {
        if (command.isBlank()) {
            logErrorAndThrowProcessBuilderException("Command couldn't be empty!")
        }
        if (command.contains(">") && useInternalRedirections) {
            logError("Found user provided redirections in `$command`. " +
                    "SAVE will create own redirections for internal purpose, please refuse redirects or use corresponding argument [redirectTo]")
        }

        // Temporary directory for stderr and stdout (posix `system()` can't separate streams, so we do it ourselves)
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY /
                ("ProcessBuilder_" + Clock.System.now().toEpochMilliseconds()).toPath())
        // Path to stdout file
        val stdoutFile = tmpDir / "stdout.txt"
        // Path to stderr file
        val stderrFile = tmpDir / "stderr.txt"
        // Instance, containing platform-dependent realization of command execution
        val processBuilderInternal = ProcessBuilderInternal(stdoutFile, stderrFile, useInternalRedirections)
        fs.createDirectories(tmpDir)
        fs.createFile(stdoutFile)
        fs.createFile(stderrFile)
        logTrace("Created temp directory $tmpDir for stderr and stdout of ProcessBuilder")

        val cmd = modifyCmd(command, directory, processBuilderInternal)

        logDebug("Executing: $cmd")
        val status = try {
            processBuilderInternal.exec(cmd)
        } catch (ex: Exception) {
            fs.deleteRecursively(tmpDir)
            logErrorAndThrowProcessBuilderException(ex.message ?: "Couldn't execute $cmd")
        }
        val stdout = fs.readLines(stdoutFile)
        val stderr = fs.readLines(stderrFile)
        fs.deleteRecursively(tmpDir)
        logTrace("Removed temp directory $tmpDir")
        if (stderr.isNotEmpty()) {
            logDebug("The following errors occurred after executing of `$command`:\t${stderr.joinToString("\t")}")
        }
        redirectTo?.let {
            fs.write(redirectTo) {
                write(stdout.joinToString("\n").encodeToByteArray())
                write(stderr.joinToString("\n").encodeToByteArray())
            }
        } ?: logTrace("Execution output:\t$stdout")
        return ExecutionResult(status, stdout, stderr)
    }

    private fun modifyCmd(
        command: String,
        directory: String,
        processBuilderInternal: ProcessBuilderInternal): String {
        // If we need to step out into some directory before execution
        val cdCmd = if (directory.isNotBlank()) {
            if (isCurrentOsWindows()) {
                "cd /d $directory && "
            } else {
                "cd $directory && "
            }
        } else {
            ""
        }
        // Additionally process command for Windows, it it contain `echo`
        val commandWithEcho = cdCmd + if (isCurrentOsWindows()) {
            processCommandWithEcho(command)
        } else {
            command
        }
        // Finally, make platform dependent adaptations
        return processBuilderInternal.prepareCmd(commandWithEcho)
    }

    /**
     * Log error message and throw exception
     *
     * @param errMsg error message
     * @throws ProcessExecutionException
     */
    private fun logErrorAndThrowProcessBuilderException(errMsg: String): Nothing {
        logError(errMsg)
        throw ProcessExecutionException(errMsg)
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
            logTrace("Modify command:`$command` to `$modifiedCommand` because of `echo` on Windows add extra newlines")
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
