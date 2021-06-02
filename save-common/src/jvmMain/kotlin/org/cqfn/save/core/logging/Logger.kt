/**
 * Platform specific utils for logging
 */

@file:JvmName("LoggerJVM")

package org.cqfn.save.core.logging

import org.cqfn.save.core.config.ResultOutputType

@Suppress(
    "WHEN_WITHOUT_ELSE",
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_ON_FUNCTION")
actual fun logToStream(
    msg: String,
    stream: ResultOutputType) {
    when (stream) {
        ResultOutputType.STDOUT -> System.out.println(msg)
        ResultOutputType.STDERR -> System.err.println(msg)
        ResultOutputType.FILE -> TODO("Not yet implemented")
    }
}
