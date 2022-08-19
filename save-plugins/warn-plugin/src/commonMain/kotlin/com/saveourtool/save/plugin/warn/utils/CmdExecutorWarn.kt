package com.saveourtool.save.plugin.warn.utils

import com.saveourtool.save.core.config.TestConfig
import com.saveourtool.save.core.files.readLines
import com.saveourtool.save.core.logging.logWarn
import com.saveourtool.save.core.plugin.ExtraFlagsExtractor
import com.saveourtool.save.core.plugin.GeneralConfig
import com.saveourtool.save.core.utils.CmdExecutorBase
import com.saveourtool.save.core.utils.ExecutionResult
import com.saveourtool.save.core.utils.ProcessBuilder
import com.saveourtool.save.plugin.warn.WarnPluginConfig

import okio.FileNotFoundException
import okio.FileSystem
import okio.Path

/**
 * Implementation of CmdExecutorBase for WarnPlugin
 */
@Suppress("LongParameterList")
class CmdExecutorWarn(
    generalConfig: GeneralConfig,
    copyPaths: List<Path>,
    extraFlagsExtractor: ExtraFlagsExtractor,
    pb: ProcessBuilder,
    execCmd: String?,
    private val execFlags: String?,
    private val batchSeparator: String,
    private val warnPluginConfig: WarnPluginConfig,
    private val testConfig: TestConfig,
    private val fs: FileSystem,
) : CmdExecutorBase(
    execCmd!!,
    generalConfig.timeOutMillis!!,
    copyPaths,
    extraFlagsExtractor,
    pb,
    testConfig
) {
    override fun getWildCardInDirectoryMode(): String? = warnPluginConfig.wildCardInDirectoryMode

    override fun getExecFlags(): String? = execFlags

    @Suppress("SwallowedException")
    override fun ExecutionResult.getStdout(): List<String> = warnPluginConfig.testToolResFileOutput?.let {
        val testToolResFilePath = testConfig.directory / warnPluginConfig.testToolResFileOutput
        try {
            fs.readLines(testToolResFilePath)
        } catch (ex: FileNotFoundException) {
            logWarn(
                "Trying to read file \"${warnPluginConfig.testToolResFileOutput}\" " +
                        "that was set as an output for a tested tool with testToolResFileOutput, " +
                        "but no such file found. Will use the stdout as an input."
            )
            // we swallow exception here to get the result from stdout
            this.stdout
        }
    }
        ?: this.stdout

    override fun getBatchSeparator(): String = batchSeparator
}
