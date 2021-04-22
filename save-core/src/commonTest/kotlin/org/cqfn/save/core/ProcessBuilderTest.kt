package org.cqfn.save.core

import org.cqfn.save.core.utils.ProcessBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("LOCAL_VARIABLE_EARLY_DECLARATION")
class ProcessBuilderTest {
    private val processBuilder = ProcessBuilder()

    @Test
    fun `check stdout`() {
        val actualResult = processBuilder.exec("echo something".split(" "), null)
        val expectedStdout = "something"
        val expectedStderr: List<String> = emptyList()
        assertEquals(0, actualResult.code)
        // posix `system()` and JVM process builder returns lines with different whitespaces, so we cut them
        assertEquals(expectedStdout, actualResult.stdout[0].trimEnd())
        assertEquals(expectedStderr, actualResult.stderr)
    }

    @Test
    fun `check stdout 2`() {
        val actualResult = processBuilder.exec("echo something >/dev/null".split(" "), null)
        val expectedStdout = "something"
        val expectedStderr: List<String> = emptyList()
        assertEquals(0, actualResult.code)
        // posix popen and JVM process builder returns lines with different whitespaces, so we cut them
        assertEquals(expectedStdout, actualResult.stdout[0].trimEnd())
        assertEquals(expectedStderr, actualResult.stderr)
    }

    @Test
    fun `check stderr`() {
        val actualResult = processBuilder.exec("ls -l root".split(" "), null)
        val expectedStdout: List<String> = emptyList()
        val expectedStderr = listOf("'ls' is not recognized as an internal or external command,",
            "operable program or batch file.")
        assertEquals(1, actualResult.code)
        assertEquals(expectedStdout, actualResult.stdout)
        assertEquals(expectedStderr, actualResult.stderr)
    }

    @Test
    fun `check stderr with additional warning`() {
        val actualResult = processBuilder.exec("ls -l root 2>/dev/null".split(" "), null)
        val expectedStdout: List<String> = emptyList()
        val expectedStderr = listOf("'ls' is not recognized as an internal or external command,",
            "operable program or batch file.")
        assertEquals(1, actualResult.code)
        assertEquals(expectedStdout, actualResult.stdout)
        assertEquals(expectedStderr, actualResult.stderr)
    }
}
