/**
 * This file contains tests for process builder, which are common for all platforms
 */

package org.cqfn.save.core

import org.cqfn.save.core.utils.ProcessBuilder
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
}
