package org.cqfn.save.cli

import org.cqfn.save.core.Save

fun main(args: Array<String>) {
    println("Starting SAVE Application...")
    val save = Save(
        createConfigFromArgs(args)
    )
    save.performAnalysis()
}
