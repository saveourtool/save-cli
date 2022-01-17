/**
 * Base classes for usage of warning adapters
 */

package org.cqfn.save.plugin.warn.adapter

import org.cqfn.save.plugin.warn.utils.Warning

import okio.Path

/**
 * Interface that is able to convert an object of generic type [T] into a [Warning].
 */
interface WarningAdapter<T> {
    /**
     * @param report an object of type [T] that should contain warnings
     * @param ctx context to use additional data in conversion process
     * @return a list of warnings decoded from [report]
     */
    fun toWarnings(report: T, ctx: AdapterContext): List<Warning>
}

/**
 * @property testRoot directory of root test config
 * @property testFiles a list of test files that can be present in the report
 */
data class AdapterContext(
    val testRoot: Path?,
    val testFiles: List<Path>,
)
