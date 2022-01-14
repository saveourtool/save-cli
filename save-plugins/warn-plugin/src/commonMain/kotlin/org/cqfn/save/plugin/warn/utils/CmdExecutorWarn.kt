package org.cqfn.save.plugin.warn.utils

import okio.FileNotFoundException
import okio.FileSystem
import okio.Path
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.logging.logWarn
import org.cqfn.save.core.plugin.*
import org.cqfn.save.core.utils.CmdExecutorBase
import org.cqfn.save.core.utils.ExecutionResult
import org.cqfn.save.core.utils.ProcessBuilder
import org.cqfn.save.plugin.warn.WarnPluginConfig

// FixMe: all plugins should use it for executing of command in the future
class CmdExecutorWarn(
    generalConfig: GeneralConfig,
    copyPaths: List<Path>,
    extraFlagsExtractor: ExtraFlagsExtractor,
    pb: ProcessBuilder,
    private val warnPluginConfig: WarnPluginConfig,
    private val testConfig: TestConfig,
    private val fs: FileSystem,
): CmdExecutorBase(generalConfig.execCmd!!, generalConfig.timeOutMillis!!, copyPaths, extraFlagsExtractor, pb, testConfig) {

    override fun wildCardInDirectoryMode(): String?  = warnPluginConfig.wildCardInDirectoryMode

    override fun execFlags(): String? = warnPluginConfig.execFlags

    override fun getStdout(execResult: ExecutionResult): List<String> = warnPluginConfig.testToolResFileOutput?.let {
        val testToolResFilePath = testConfig.directory / warnPluginConfig.testToolResFileOutput
        try {
            fs.readLines(testToolResFilePath)
        } catch (ex: FileNotFoundException) {
            logWarn(
                "Trying to read file \"${warnPluginConfig.testToolResFileOutput}\" " +
                        "that was set as an output for a tested tool with testToolResFileOutput, " +
                        "but no such file found. Will use the stdout as an input."
            )
            execResult.stdout
        }
    }
    ?: execResult.stdout


    override fun batchSeparator(): String = warnPluginConfig.batchSeparator!!
}
