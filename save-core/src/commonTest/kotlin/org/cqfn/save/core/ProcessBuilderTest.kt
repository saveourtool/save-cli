/**
 * This file contains tests for process builder, which are common for all platforms
 */

package org.cqfn.save.core

import org.cqfn.save.core.utils.ProcessBuilder
import org.cqfn.save.core.utils.ProcessBuilder.Companion.processCommandWithEcho
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("INLINE_CLASS_CAN_BE_USED", "LOCAL_VARIABLE_EARLY_DECLARATION")
class ProcessBuilderTest {
    private val processBuilder = ProcessBuilder()

    @Test
    fun `empty command`() {
        val actualResult = processBuilder.exec(" ", null)
        assertEquals(-1, actualResult.code)
        assertEquals(emptyList(), actualResult.stdout)
        assertEquals(listOf("Command couldn't be empty!"), actualResult.stderr)
    }

    @Test
    fun `check stdout`() {
        val actualResult = processBuilder.exec("echo something", null)
        val expectedCode = 0
        val expectedStdout = "something"
        val expectedStderr: List<String> = emptyList()
        assertEquals(expectedCode, actualResult.code)
        // posix `system()` and JVM process builder returns lines with different whitespaces, so we cut them
        assertEquals(expectedStdout, actualResult.stdout[0].trimEnd())
        assertEquals(expectedStderr, actualResult.stderr)
    }

    @Test
    fun `simple check`() {
        val inputCommand = "echo something"
        val expectedCommand = "echo | set /p=\"something\""
        assertEquals(expectedCommand, processCommandWithEcho(inputCommand))
    }

    @Test
    fun `simple check with redirection`() {
        val inputCommand = "echo something > /dev/null"
        val expectedCommand = "echo | set /p=\"something\" > /dev/null"
        assertEquals(expectedCommand, processCommandWithEcho(inputCommand))
    }

    @Test
    fun `simple check with redirection without first whitespace`() {
        val inputCommand = "echo something> /dev/null"
        val expectedCommand = "echo | set /p=\"something\" > /dev/null"
        assertEquals(expectedCommand, processCommandWithEcho(inputCommand))
    }

    @Test
    fun `simple check with redirection without whitespaces at all`() {
        val inputCommand = "echo something>/dev/null"
        val expectedCommand = "echo | set /p=\"something\" >/dev/null"
        assertEquals(expectedCommand, processCommandWithEcho(inputCommand))
    }

    @Test
    fun `change multiple echo commands with redirections`() {
        val inputCommand = "echo a > /dev/null && echo b 2>/dev/null && ls"
        val expectedCommand = "echo | set /p=\"a\" > /dev/null && echo | set /p=\"b\" 2>/dev/null && ls"
        assertEquals(expectedCommand, processCommandWithEcho(inputCommand))
    }
}
