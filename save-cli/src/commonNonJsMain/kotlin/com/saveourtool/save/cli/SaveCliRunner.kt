/**
 * Main entry point for SAVE CLI execution
 */

package com.saveourtool.save.cli

import com.saveourtool.save.cli.config.of
import com.saveourtool.save.core.Save
import com.saveourtool.save.core.config.SaveProperties

fun main(args: Array<String>) {
    val config = SaveProperties.of(args)
    Save(config, fs)
        .performAnalysis()
}
