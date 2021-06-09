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
    private val processBuilder = ProcessBuilder(useInternalRedirections = true)

    @Test
    fun `check stderr`() {
        val actualResult = processBuilder.exec("cd non_existent_dir", null)
        val expectedStdout: List<String> = emptyList()
        var expectedCode: Int
        lateinit var expectedStderr: List<String>
        when (getCurrentOs()) {
            CurrentOs.LINUX -> {
                expectedCode = 2
                expectedStderr = listOf("sh: 1: cd: can't cd to non_existent_dir")
            }
            CurrentOs.MACOS -> {
                expectedCode = 1
                expectedStderr = listOf("sh: line 0: cd: non_existent_dir: No such file or directory")
            }
            CurrentOs.WINDOWS -> {
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
        when (getCurrentOs()) {
            CurrentOs.LINUX -> {
                expectedCode = 2
                expectedStderr = emptyList()
            }
            CurrentOs.MACOS -> {
                expectedCode = 1
                expectedStderr = emptyList()
            }
            CurrentOs.WINDOWS -> {
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
