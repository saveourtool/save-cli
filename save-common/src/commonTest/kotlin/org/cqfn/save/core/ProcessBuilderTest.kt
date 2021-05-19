/**
 * This file contains tests for process builder, which are common for all platforms
 */

package org.cqfn.save.core

import org.cqfn.save.core.utils.CurrentOs
import org.cqfn.save.core.utils.ProcessBuilder
import org.cqfn.save.core.utils.ProcessBuilder.Companion.processCommandWithEcho
import org.cqfn.save.core.utils.getCurrentOs
import org.cqfn.save.core.utils.isCurrentOsWindows
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
    fun `debug`() {
        processBuilder.exec("file /tmp", null)
        processBuilder.exec("ls ~/", null)
        if (getCurrentOs() == CurrentOs.MACOS) {
            assertEquals(1, 0)
        }
    }

    @Test
    fun `check stdout`() {
        val actualResult = processBuilder.exec("echo something", null)
        val expectedCode = if (isCurrentOsWindows()) 1 else 0
        val expectedStdout = listOf("something")
        val expectedStderr: List<String> = emptyList()
        assertEquals(expectedCode, actualResult.code)
        assertEquals(expectedStdout, actualResult.stdout)
        assertEquals(expectedStderr, actualResult.stderr)
    }

    @Test
    fun `command without echo`() {
        val inputCommand = "cd /some/dir; cat /some/file ; ls"
        assertEquals(inputCommand, processCommandWithEcho(inputCommand))
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
    fun `one long echo`() {
        val inputCommand = "echo stub STUB stub foo bar "
        val expectedCommand = "echo | set /p=\"stub STUB stub foo bar\""
        assertEquals(expectedCommand, processCommandWithEcho(inputCommand))
    }

    @Test
    fun `change multiple echo commands with redirections`() {
        val inputCommand = "echo a > /dev/null && echo b 2>/dev/null && ls"
        val expectedCommand = "echo | set /p=\"a\" > /dev/null && echo | set /p=\"b\" 2>/dev/null && ls"
        assertEquals(expectedCommand, processCommandWithEcho(inputCommand))
    }

    @Test
    fun `change multiple echo commands with redirections 2`() {
        val inputCommand = "echo a > /dev/null ; echo b 2>/dev/null ; ls"
        val expectedCommand = "echo | set /p=\"a\" > /dev/null ; echo | set /p=\"b\" 2>/dev/null ; ls"
        assertEquals(expectedCommand, processCommandWithEcho(inputCommand))
    }

    @Test
    fun `change multiple echo commands with redirections 3`() {
        val inputCommand = "echo a > /dev/null; echo b 2>/dev/null; ls"
        val expectedCommand = "echo | set /p=\"a\" > /dev/null ; echo | set /p=\"b\" 2>/dev/null ; ls"
        assertEquals(expectedCommand, processCommandWithEcho(inputCommand))
    }

    @Test
    fun `extra whitespaces shouldn't influence to echo`() {
        val inputCommand = "echo foo bar ; echo b; ls"
        val expectedCommand = "echo | set /p=\"foo bar\"  ; echo | set /p=\"b\"  ; ls"
        assertEquals(expectedCommand, processCommandWithEcho(inputCommand))
    }
}
