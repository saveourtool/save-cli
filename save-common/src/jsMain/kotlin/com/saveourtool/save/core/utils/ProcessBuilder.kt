/**
 * Implementation of [ProcessBuilderInternal] for JS
 */

package com.saveourtool.save.core.utils

import okio.Path

actual fun createProcessBuilderInternal(
    stdoutFile: Path,
    stderrFile: Path,
    useInternalRedirections: Boolean,
): ProcessBuilderInternal = object : ProcessBuilderInternal {
    override fun prepareCmd(command: String): String = error("Not implemented for JS")

    override fun exec(cmd: String, timeOutMillis: Long): Int = error("Not implemented for JS")
}
