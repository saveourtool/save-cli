@file:JvmName("LoggerJVM")
package org.cqfn.save.core.logging

import org.cqfn.save.core.config.ResultOutputType

actual fun logToStream(level: String, msg: String, output: ResultOutputType) {
    when (output) {
        ResultOutputType.STDOUT -> System.out.println(msg)
        ResultOutputType.STDERR -> System.err.println(msg)
        ResultOutputType.FILE -> System.out.println(msg)
    }
}