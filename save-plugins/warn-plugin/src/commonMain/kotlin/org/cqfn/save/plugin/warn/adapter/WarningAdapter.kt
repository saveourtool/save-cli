package org.cqfn.save.plugin.warn.adapter

import okio.Path
import org.cqfn.save.plugin.warn.utils.Warning

interface WarningAdapter<T> {
    fun toWarnings(report: T, ctx: AdapterContext): List<Warning>
}

data class AdapterContext(
    val testRoot: Path?,
    val testFiles: List<Path>,
)
