@file:JvmName("PlatformUtilsJVM")
@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_ON_FUNCTION",
    "FILE_NAME_MATCH_CLASS",
    "MatchingDeclarationName")

package org.cqfn.save.core.utils

import org.cqfn.save.core.config.ResultOutputType

import okio.Buffer
import okio.Sink
import okio.Timeout

actual class StdStreamsSink actual constructor(private val outputType: ResultOutputType) : Sink {
    actual override fun close() = Unit

    actual override fun flush() = Unit

    actual override fun timeout(): Timeout = Timeout.NONE

    actual override fun write(source: Buffer, byteCount: Long) {
        val msg = source.readByteString(byteCount).utf8()
        writeToStream(msg, outputType)
    }
}

actual fun getCurrentOs() = when {
    System.getProperty("os.name").startsWith("Linux", ignoreCase = true) -> CurrentOs.LINUX
    System.getProperty("os.name").startsWith("Mac", ignoreCase = true) -> CurrentOs.MACOS
    System.getProperty("os.name").startsWith("Windows", ignoreCase = true) -> CurrentOs.WINDOWS
    else -> CurrentOs.UNDEFINED
}

actual fun writeToStream(msg: String, outputType: ResultOutputType) {
    when (outputType) {
        ResultOutputType.STDOUT -> System.out.println(msg)
        ResultOutputType.STDERR -> System.err.println(msg)
        else -> TODO("Not yet supported")
    }
}

actual typealias AtomicInt = java.util.concurrent.atomic.AtomicInteger
