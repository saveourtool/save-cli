package org.cqfn.save.core.utils

import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("LOCAL_VARIABLE_EARLY_DECLARATION",
    "SAY_NO_TO_VAR",
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_ON_FUNCTION"
)
class ProcessBuilderInternalTest {
    private val processBuilder = ProcessBuilder()

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
    fun `check stdout with redirection`() {
        val actualResult = processBuilder.exec("echo something >/dev/null", null)
        var expectedCode: Int
        val expectedStdout = ""
        lateinit var expectedStderr: List<String>
        when {
            System.getProperty("os.name").contains("Linux", ignoreCase = true) ||
                    System.getProperty("os.name").contains("Mac", ignoreCase = true) -> {
                expectedCode = 0
                expectedStderr = emptyList()
            }
            System.getProperty("os.name").contains("Windows", ignoreCase = true) -> {
                expectedCode = 1
                expectedStderr = listOf("The system cannot find the path specified.")
            }
            else -> return
        }
        assertEquals(expectedCode, actualResult.code)
        // posix popen and JVM process builder returns lines with different whitespaces, so we cut them
        assertEquals(expectedStdout, "")
        assertEquals(expectedStderr, actualResult.stderr)
    }

    @Test
    fun `check stderr`() {
        val actualResult = processBuilder.exec("cd non_existent_dir", null)
        val expectedStdout: List<String> = emptyList()
        var expectedCode: Int
        lateinit var expectedStderr: List<String>
        when {
            System.getProperty("os.name").contains("Linux", ignoreCase = true) ||
                    System.getProperty("os.name").contains("Mac", ignoreCase = true) -> {
                expectedCode = 2
                expectedStderr = listOf("sh: 1: cd: can't cd to non_existent_dir")
            }
            System.getProperty("os.name").contains("Windows", ignoreCase = true) -> {
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
        val actualResult = processBuilder.exec("cd non_existent_dir 2>/dev/null", null)
        val expectedStdout: List<String> = emptyList()
        var expectedCode: Int
        lateinit var expectedStderr: List<String>
        when {
            System.getProperty("os.name").contains("Linux", ignoreCase = true) ||
                    System.getProperty("os.name").contains("Mac", ignoreCase = true) -> {
                expectedCode = 2
                expectedStderr = emptyList()
            }
            System.getProperty("os.name").contains("Windows", ignoreCase = true) -> {
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
