/**
 * This file contains tests for process builder, which are common for all platforms
 */

package com.saveourtool.save.core

import com.saveourtool.save.core.files.fs
import com.saveourtool.save.core.utils.ProcessBuilder
import com.saveourtool.save.core.utils.ProcessBuilder.Companion.processCommandWithEcho
import com.saveourtool.save.core.utils.ProcessExecutionException
import com.saveourtool.save.core.utils.isCurrentOsWindows
import kotlin.js.JsName

import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("INLINE_CLASS_CAN_BE_USED")
class ProcessBuilderTest {
    private val processBuilder = ProcessBuilder(useInternalRedirections = true, fs)

    @Test
    @JsName("empty_command")
    fun `empty command`() {
        try {
            processBuilder.exec(" ", "", null, 10_000L)
        } catch (ex: ProcessExecutionException) {
            assertEquals("Execution command in ProcessBuilder couldn't be empty!", ex.message)
        }
    }

    @Test
    @JsName("check_stdout")
    fun `check stdout`() {
        val actualResult = processBuilder.exec("echo something", "", null, 10_000L)
        val expectedCode = 0
        val expectedStdout = listOf("something")
        assertEquals(expectedCode, actualResult.code)
        assertEquals(expectedStdout, actualResult.stdout)
        assertEquals(emptyList(), actualResult.stderr)
    }

    @Test
    @Suppress("SAY_NO_TO_VAR")
    @JsName("check_stdout_with_redirection")
    fun `check stdout with redirection`() {
        val actualResult = processBuilder.exec("echo something >/dev/null", "", null, 10_000L)
        val expectedCode: Int
        lateinit var expectedStderr: List<String>
        when {
            isCurrentOsWindows() -> {
                expectedCode = 1
                expectedStderr = listOf("The system cannot find the path specified.")
            }
            else -> {
                expectedCode = 0
                expectedStderr = emptyList()
            }
        }
        assertEquals(expectedCode, actualResult.code)
        assertEquals(emptyList(), actualResult.stdout)
        assertEquals(expectedStderr, actualResult.stderr)
    }

    @Test
    @JsName("command_without_echo")
    fun `command without echo`() {
        val inputCommand = "cd /some/dir; cat /some/file ; ls"
        assertEquals(inputCommand, processCommandWithEcho(inputCommand))
    }

    @Test
    @JsName("simple_check")
    fun `simple check`() {
        val inputCommand = "echo something"
        val expectedCommand = "echo | set /p dummyName=\"something\""
        assertEquals(expectedCommand, processCommandWithEcho(inputCommand))
    }

    @Test
    @JsName("simple_check_with_redirection")
    fun `simple check with redirection`() {
        val inputCommand = "echo something > /dev/null"
        val expectedCommand = "echo | set /p dummyName=\"something\" > /dev/null"
        assertEquals(expectedCommand, processCommandWithEcho(inputCommand))
    }

    @Test
    @JsName("simple_check_with_redirection_without_first_whitespace")
    fun `simple check with redirection without first whitespace`() {
        val inputCommand = "echo something> /dev/null"
        val expectedCommand = "echo | set /p dummyName=\"something\" > /dev/null"
        assertEquals(expectedCommand, processCommandWithEcho(inputCommand))
    }

    @Test
    @JsName("simple_check_with_redirection_without_whitespaces_at_all")
    fun `simple check with redirection without whitespaces at all`() {
        val inputCommand = "echo something>/dev/null"
        val expectedCommand = "echo | set /p dummyName=\"something\" >/dev/null"
        assertEquals(expectedCommand, processCommandWithEcho(inputCommand))
    }

    @Test
    @JsName("one_long_echo")
    fun `one long echo`() {
        val inputCommand = "echo stub STUB stub foo bar "
        val expectedCommand = "echo | set /p dummyName=\"stub STUB stub foo bar\""
        assertEquals(expectedCommand, processCommandWithEcho(inputCommand))
    }

    @Test
    @JsName("change_multiple_echo_commands_with_redirections")
    fun `change multiple echo commands with redirections`() {
        val inputCommand = "echo a > /dev/null && echo b 2>/dev/null && ls"
        val expectedCommand = "echo | set /p dummyName=\"a\" > /dev/null && echo | set /p dummyName=\"b\" 2>/dev/null && ls"
        assertEquals(expectedCommand, processCommandWithEcho(inputCommand))
    }

    @Test
    @JsName("change_multiple_echo_commands_with_redirections_2")
    fun `change multiple echo commands with redirections 2`() {
        val inputCommand = "echo a > /dev/null ; echo b 2>/dev/null ; ls"
        val expectedCommand = "echo | set /p dummyName=\"a\" > /dev/null ; echo | set /p dummyName=\"b\" 2>/dev/null ; ls"
        assertEquals(expectedCommand, processCommandWithEcho(inputCommand))
    }

    @Test
    @JsName("change_multiple_echo_commands_with_redirections_3")
    fun `change multiple echo commands with redirections 3`() {
        val inputCommand = "echo a > /dev/null; echo b 2>/dev/null; ls"
        val expectedCommand = "echo | set /p dummyName=\"a\" > /dev/null ; echo | set /p dummyName=\"b\" 2>/dev/null ; ls"
        assertEquals(expectedCommand, processCommandWithEcho(inputCommand))
    }

    @Test
    @JsName("extra_whitespaces_shouldn_t_influence_to_echo")
    fun `extra whitespaces shouldn't influence to echo`() {
        val inputCommand = "echo foo bar ; echo b; ls"
        val expectedCommand = "echo | set /p dummyName=\"foo bar\"  ; echo | set /p dummyName=\"b\"  ; ls"
        assertEquals(expectedCommand, processCommandWithEcho(inputCommand))
    }
}
