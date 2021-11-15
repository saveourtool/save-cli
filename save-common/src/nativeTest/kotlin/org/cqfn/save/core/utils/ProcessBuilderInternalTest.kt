package org.cqfn.save.core.utils

import okio.FileSystem
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("SAY_NO_TO_VAR")
class ProcessBuilderInternalTest {
    private val processBuilder = ProcessBuilder(useInternalRedirections = true, FileSystem.SYSTEM)

    @Test
    fun `check stderr`() {
        val actualResult = processBuilder.exec("cd non_existent_dir", "", null, 10_000L)
        lateinit var expectedStderr: List<String>
        val expectedCode: Int
        when (getCurrentOs()) {
            CurrentOs.LINUX -> {
                expectedCode = 512
                expectedStderr = listOf("sh: 1: cd: can't cd to non_existent_dir")
            }
            CurrentOs.MACOS -> {
                expectedCode = 256
                expectedStderr = listOf("sh: line 0: cd: non_existent_dir: No such file or directory")
            }
            CurrentOs.WINDOWS -> {
                expectedCode = 1
                expectedStderr = listOf("The system cannot find the path specified.")
            }
            else -> return
        }
        assertEquals(expectedCode, actualResult.code)
        assertEquals(emptyList(), actualResult.stdout)
        assertEquals(expectedStderr, actualResult.stderr)
    }

    @Test
    fun `check stderr with additional warning`() {
        val actualResult = processBuilder.exec("cd non_existent_dir 2>/dev/null", "", null, 10_000L)
        lateinit var expectedStderr: List<String>
        val expectedCode: Int
        when (getCurrentOs()) {
            CurrentOs.LINUX -> {
                expectedCode = 512
                expectedStderr = emptyList()
            }
            CurrentOs.MACOS -> {
                expectedCode = 256
                expectedStderr = emptyList()
            }
            CurrentOs.WINDOWS -> {
                expectedCode = 1
                expectedStderr = listOf("The system cannot find the path specified.")
            }
            else -> return
        }
        assertEquals(expectedCode, actualResult.code)
        assertEquals(emptyList(), actualResult.stdout)
        assertEquals(expectedStderr, actualResult.stderr)
    }
}
