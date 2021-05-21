/**
 * Main entry point for SAVE CLI execution
 */

package org.cqfn.save.cli

import org.cqfn.save.core.Save

fun main(args: Array<String>) {
    val args = arrayOf("tomlConfig", "D:\\projects\\save\\examples")
    val config = createConfigFromArgs(args)
    Save(config)
        .performAnalysis()
}
