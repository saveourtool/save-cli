package org.cqfn.save.cli

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import org.cqfn.save.core.config.ReportType
import org.cqfn.save.core.config.SaveConfig

fun createConfigFromArgs(args: Array<String>): SaveConfig {
    val parser = ArgParser("save")

    val fileName by parser.option(
        ArgType.String,
        shortName = "f",
        description = "Name of save config files",
    ).default("save.toml")

    val debug by parser.option(
        ArgType.Boolean,
        shortName = "d",
        description = "Turn on debug logging"
    ).default(false)

    val quiet by parser.option(
        ArgType.Boolean,
        shortName = "q",
        description = "Do not log anything"
    ).default(false)

    val reportType by parser.option(
        ArgType.Choice<ReportType>(),
        shortName = "r",
    ).default(ReportType.JSON)

    parser.parse(args)
    return SaveConfig(
        debug = debug,
        quiet = quiet,
        reportType = reportType
    )
}
