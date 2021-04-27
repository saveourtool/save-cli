package org.cqfn.save.core.utils

import okio.FileSystem
import okio.Path
import org.cqfn.save.core.logging.logDebug
import java.io.BufferedReader
import java.io.InputStreamReader

import java.lang.ProcessBuilder
import java.util.stream.Collectors

@Suppress("MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_ON_FUNCTION"
)
actual class ProcessBuilderInternal {
    private val pb = ProcessBuilder()
    lateinit var stderrFile: Path
    lateinit var stdoutFile: Path
    lateinit var cmdArray: Array<String>

    actual fun prepareCmd(command: String, collectStdout: Boolean, stdoutFile: Path, stderrFile: Path): String {
        val shell = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) arrayOf("CMD", "/C") else arrayOf("sh", "-c")
        val cmd = arrayOf(*shell, command)
        this.stdoutFile = stdoutFile
        this.stderrFile = stderrFile
        this.cmdArray = cmd
        return cmd.joinToString()
    }

    actual fun exec(cmd: String): Int {
        logDebug("Executing: $cmd")
        val runTime = Runtime.getRuntime()
        val tempCmd: Array<String> = cmdArray
        val process = runTime.exec(cmdArray)//cmd.split(",").toTypedArray())
        writeDataFromBufferToFile(process, "stdout", stdoutFile)
        writeDataFromBufferToFile(process, "stderr", stderrFile)
        // TODO: Does waitFor() == -1 responsible for errors like in posix?
        return process.waitFor()
    }

    private fun writeDataFromBufferToFile(process: Process, stream: String, file: Path) {
        val br = if (stream == "stdout") {
            BufferedReader(InputStreamReader(process.inputStream))
        }
        else {
            BufferedReader(InputStreamReader(process.errorStream))
        }
        val data = br.lines().collect(Collectors.joining("\n"))
        FileSystem.SYSTEM.write(file) {
            write(data.encodeToByteArray())
        }
        println("data: $stream $data")
    }
}

