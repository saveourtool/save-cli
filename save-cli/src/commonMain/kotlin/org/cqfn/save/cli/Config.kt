/**
 * Utilities to work with SAVE config in CLI mode
 */

package org.cqfn.save.cli

import org.cqfn.save.core.config.ReportType
import org.cqfn.save.core.config.SaveConfig

import okio.Path.Companion.toPath

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default

/**
 * @param args CLI args
 * @return an instance of [SaveConfig]
 */
fun createConfigFromArgs(args: Array<String>): SaveConfig {
    val parser = ArgParser("save")

    val configPath by parser.option(
        ArgType.String,
        shortName = "f",
        description = "Path to the root save config file",
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
        configPath = configPath.toPath(),
        debug = debug,
        quiet = quiet,
        reportType = reportType
    )
}
