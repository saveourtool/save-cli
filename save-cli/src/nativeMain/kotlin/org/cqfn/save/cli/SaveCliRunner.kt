/**
 * Main entry point for SAVE CLI execution
 */

package org.cqfn.save.cli

import org.cqfn.save.core.Save

fun main(args: Array<String>) {
    val save = Save(
        createConfigFromArgs(args)
    )
    save.performAnalysis()
}
