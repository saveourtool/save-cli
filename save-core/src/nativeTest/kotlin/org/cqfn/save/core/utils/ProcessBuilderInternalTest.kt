package org.cqfn.save.core.utils

import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("LOCAL_VARIABLE_EARLY_DECLARATION")
class ProcessBuilderInternalTest {
    private val processBuilder = ProcessBuilder()

    @Test
    fun `check stdout`() {
        val actualResult = processBuilder.exec("echo something".split(" "), null)
        val expectedCode = 0
        val expectedStdout = "something"
        val expectedStderr: List<String> = emptyList()
        assertEquals(expectedCode, actualResult.code)
        // posix `system()` and JVM process builder returns lines with different whitespaces, so we cut them
        assertEquals(expectedStdout, actualResult.stdout[0].trimEnd())
        assertEquals(expectedStderr, actualResult.stderr)
    }

    @Test
    fun `check stdout with redirection`() {
        val actualResult = processBuilder.exec("echo something >/dev/null".split(" "), null)
        val expectedCode = 0
        val expectedStdout = "something"
        val expectedStderr: List<String> = emptyList()
        assertEquals(expectedCode, actualResult.code)
        // posix popen and JVM process builder returns lines with different whitespaces, so we cut them
        assertEquals(expectedStdout, actualResult.stdout[0].trimEnd())
        assertEquals(expectedStderr, actualResult.stderr)
    }

    @Test
    fun `check stderr`() {
        val actualResult = processBuilder.exec("cd non_existent_dir".split(" "), null)
        val expectedStdout: List<String> = emptyList()
        lateinit var expectedStderr: List<String>
        var expectedCode: Int
        when (Platform.osFamily) {
            OsFamily.LINUX, OsFamily.MACOSX -> {
                expectedCode = 512
                expectedStderr = listOf("sh: 1: cd: can't cd to non_existent_dir")
            }
            OsFamily.WINDOWS -> {
                expectedCode = 1
                expectedStderr = listOf("The system cannot find the path specified.")
            }
            else -> return
        }
        assertEquals(expectedCode, actualResult.code)
        assertEquals(expectedStdout, actualResult.stdout)
        assertEquals(expectedStderr, actualResult.stderr)
    }

    @Test
    fun `check stderr with additional warning`() {
        val actualResult = processBuilder.exec("cd non_existent_dir 2>/dev/null".split(" "), null)
        val expectedStdout: List<String> = emptyList()
        lateinit var expectedStderr: List<String>
        var expectedCode: Int
        when (Platform.osFamily) {
            OsFamily.LINUX, OsFamily.MACOSX -> {
                expectedCode = 512
                expectedStderr = listOf("sh: 1: cd: can't cd to non_existent_dir")
            }
            OsFamily.WINDOWS -> {
                expectedCode = 1
                expectedStderr = listOf("The system cannot find the path specified.")
            }
            else -> return
        }
        assertEquals(expectedCode, actualResult.code)
        assertEquals(expectedStdout, actualResult.stdout)
        assertEquals(expectedStderr, actualResult.stderr)
    }
}
