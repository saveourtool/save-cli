/**
 * Main entry point for SAVE CLI execution
 */

package org.cqfn.save.cli

import org.cqfn.save.core.Save
import okio.FileSystem

fun main(args: Array<String>) {
    val config = createConfigFromArgs(args)
    Save(config, FileSystem.SYSTEM)
        .performAnalysis()
}
