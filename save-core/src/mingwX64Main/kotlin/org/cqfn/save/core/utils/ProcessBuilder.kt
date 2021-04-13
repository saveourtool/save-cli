@file:Suppress("FILE_WILDCARD_IMPORTS")

package org.cqfn.save.core.utils

import okio.FileSystem
import okio.Path
import platform.posix.fgets
import platform.posix.pclose
import platform.posix.popen

import kotlinx.cinterop.MemScope
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.placeTo
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString

@Suppress("MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_ON_FUNCTION"
)
actual class ProcessBuilder {
    actual fun exec(command: List<String>, redirectTo: Path?): ExecutionResult {
        val pd = popen!!.invoke(command.joinToString(" ").cstr.placeTo(MemScope()), "r".cstr.placeTo(MemScope()))

        val stdout = buildString {
            val buffer = ByteArray(4096)
            while (fgets(buffer.refTo(0), buffer.size, pd) != null) {
                append(buffer.toKString())
            }
        }

        val status = pclose!!.invoke(pd)
        if (status != 0) {
            error("Command `$command` failed with status $status: $stdout")
        }
        println(stdout)
        redirectTo?.let {
            FileSystem.SYSTEM.write(redirectTo) {
                write(stdout.encodeToByteArray())
            }
        }
        return ExecutionResult(0, stdout.split("\n"), emptyList())
    }
}
