package org.cqfn.save.core.utils

import okio.Path
import java.io.File
import java.lang.ProcessBuilder

@Suppress("MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_ON_FUNCTION"
)
actual class ProcessBuilder {
    private val pb = ProcessBuilder()

    actual fun exec(command: List<String>, redirectTo: Path?): ExecutionResult {
        val shell = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) listOf("CMD", "/C") else listOf("sh", "-c")
        val code = pb.command(shell + listOf(command.joinToString(" ")))
            .let { builder ->
                redirectTo?.let {
                    builder.redirectOutput(File(it.name))
                }
                    ?: builder
            }
            .redirectErrorStream(true)
            .start()
            .waitFor()
        return ExecutionResult(code, redirectTo?.let { File(it.name).readLines() } ?: emptyList(), emptyList())
    }
}
