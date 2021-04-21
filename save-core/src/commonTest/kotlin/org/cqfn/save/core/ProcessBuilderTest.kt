package org.cqfn.save.core

import org.cqfn.save.core.utils.ProcessBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

class ProcessBuilderTest {
    private val processBuilder = ProcessBuilder()

    @Test
    fun `simple check`() {
        val actualResult = processBuilder.exec("ls -l root 2>/dev/null".split(" "), null)
        val expectedStdout: List<String> = listOf("")
        val expectedStderr = listOf("'ls' is not recognized as an internal or external command,",
            "operable program or batch file.")
        assertEquals(1, actualResult.code)
        assertEquals(expectedStdout, actualResult.stdout)
        assertEquals(expectedStderr, actualResult.stderr)
    }

}