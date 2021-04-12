/**
 * Utilities to run a process and get its result.
 */

package org.cqfn.save.core.utils

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
     * @param redirectTo a file where process output should be redirected too. If null, output will e returned as [ExecutionResult.stdout].
     * @return [ExecutionResult] built from process output
     */
    fun exec(command: List<String>, redirectTo: Path?): ExecutionResult
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
