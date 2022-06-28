/**
 * Main entry point for SAVE CLI execution
 */

package com.saveourtool.save.cli

import com.saveourtool.save.core.Save
import com.saveourtool.save.core.config.SaveProperties
import com.saveourtool.save.core.config.of
import okio.FileSystem

fun main(args: Array<String>) {
    val config = SaveProperties.of(args)
    Save(config, FileSystem.SYSTEM)
        .performAnalysis()
}
