/**
 * Main entry point for SAVE CLI execution
 */

package com.saveourtool.save.cli

import com.saveourtool.save.core.Save
import okio.FileSystem

fun main(args: Array<String>) {
    val config = createConfigFromArgs(args)
    Save(config, FileSystem.SYSTEM)
        .performAnalysis()
}
