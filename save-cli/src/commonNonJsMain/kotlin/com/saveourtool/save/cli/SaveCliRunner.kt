/**
 * Main entry point for SAVE CLI execution
 */

package com.saveourtool.save.cli

import com.saveourtool.save.core.Save

fun main(args: Array<String>) {
    val config = createConfigFromArgs(args)
    Save(config, fs)
        .performAnalysis()
}
