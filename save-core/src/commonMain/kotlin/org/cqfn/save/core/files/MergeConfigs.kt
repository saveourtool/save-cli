package org.cqfn.save.core.files

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.plugin.PluginConfig
import org.cqfn.save.plugin.warn.WarnPluginConfig
import org.cqfn.save.plugins.fix.FixPluginConfig

/**
 * A class that is capable for merging inherited configurations
 */
class MergeConfigs {
    /**
     * Merge parent configurations with current and prolong it for all child configs
     *
     * @param testConfig - testing SAVE config (save.toml) which should be merged
     */
    fun merge(testConfig: TestConfig) {
        logDebug("Start merge configs for ${testConfig.location}")
        val parentConfigs = collectParentConfigs(testConfig)
        mergeConfigList(parentConfigs)
        val childConfigs = collectChildConfigs(testConfig)
        //mergeConfigList(childConfigs)
    }

    // Create the list of parent configs
    private fun collectParentConfigs(testConfig: TestConfig): MutableList<TestConfig> {
        val configList = mutableListOf(testConfig)
        var parentConfig = testConfig.parentConfig
        while (parentConfig != null) {
            configList.add(parentConfig)
            parentConfig = parentConfig.parentConfig
        }
        configList.reverse()
        return configList
    }

    // Create the list of child configs
    private fun collectChildConfigs(testConfig: TestConfig): MutableList<MutableList<TestConfig>> {
        val configBranches = mutableListOf<MutableList<TestConfig>>()
        val currentBranch = mutableListOf(testConfig)
        return mutableListOf(mutableListOf(testConfig))
    }

    // Merge configurations
    private fun mergeConfigList(configList: MutableList<TestConfig>) {
        if (configList.size == 1) {
            return
        }
        val pairs = configList.zipWithNext()

        pairs.forEach { (parentConfig, childConfig) ->
            logDebug("Merging ${parentConfig.location} with ${childConfig.location}")
            mergeChildConfigWithParent(parentConfig.pluginConfigs, childConfig.pluginConfigs)
        }
    }

    private fun mergeChildConfigWithParent(parentConfig: MutableList<PluginConfig>, childConfig: MutableList<PluginConfig>) {
        // Create the list of corresponding configs, if some of them will be null -> list will contain only one element,
        // which we apply as final config, otherwise we will merge configs
        val generalConfigs = listOfNotNull(
            parentConfig.filterIsInstance<GeneralConfig>().firstOrNull(),
            childConfig.filterIsInstance<GeneralConfig>().firstOrNull()
        )
        val newGeneralConfig: GeneralConfig? = if (generalConfigs.size != 2) generalConfigs.firstOrNull() else mergeGeneralConfigs(generalConfigs.first(), generalConfigs.last())

        val warnConfigs = listOfNotNull(
            parentConfig.filterIsInstance<WarnPluginConfig>().firstOrNull(),
            childConfig.filterIsInstance<WarnPluginConfig>().firstOrNull()
        )
        val newWarnConfig: WarnPluginConfig? = if (warnConfigs.size != 2) warnConfigs.firstOrNull() else mergeWarnConfigs(warnConfigs.first(), warnConfigs.last())

        val fixConfigs = listOfNotNull(
            parentConfig.filterIsInstance<FixPluginConfig>().firstOrNull(),
            childConfig.filterIsInstance<FixPluginConfig>().firstOrNull()
        )
        val newFixConfig: FixPluginConfig? = if (fixConfigs.size != 2) fixConfigs.firstOrNull() else mergeFixConfigs(fixConfigs.first(), fixConfigs.last())

        childConfig.clear()
        val result = listOfNotNull(newGeneralConfig, newWarnConfig, newFixConfig)
        result.forEach { childConfig.add(it) }
    }

    private fun mergeGeneralConfigs(parentConfig: GeneralConfig, childConfig: GeneralConfig): GeneralConfig {
        return GeneralConfig(
            if (childConfig.tags != null) childConfig.tags else parentConfig.tags,
            if (childConfig.description != null) childConfig.description else parentConfig.description,
            if (childConfig.excludedTests != null) childConfig.excludedTests else parentConfig.excludedTests,
            if (childConfig.includedTests != null) childConfig.includedTests else parentConfig.includedTests,
        )
    }

    private fun mergeWarnConfigs(parentConfig: WarnPluginConfig, childConfig: WarnPluginConfig): WarnPluginConfig {
        return WarnPluginConfig(
            if (childConfig.execCmd != null) childConfig.execCmd else parentConfig.execCmd,
            if (childConfig.warningsInputPattern != null) childConfig.warningsInputPattern else parentConfig.warningsInputPattern,
            if (childConfig.warningsOutputPattern != null) childConfig.warningsOutputPattern else parentConfig.warningsOutputPattern,
            if (childConfig.warningTextHasLine != null) childConfig.warningTextHasLine else parentConfig.warningTextHasLine,
            if (childConfig.warningTextHasColumn != null) childConfig.warningTextHasColumn else parentConfig.warningTextHasColumn,
            if (childConfig.lineCaptureGroup != null) childConfig.lineCaptureGroup else parentConfig.lineCaptureGroup,
            if (childConfig.columnCaptureGroup != null) childConfig.columnCaptureGroup else parentConfig.columnCaptureGroup,
            if (childConfig.messageCaptureGroup != null) childConfig.messageCaptureGroup else parentConfig.messageCaptureGroup,
        )
    }

    private fun mergeFixConfigs(parentConfig: FixPluginConfig, childConfig: FixPluginConfig): FixPluginConfig {
        return FixPluginConfig(
            if (childConfig.execCmd != null) childConfig.execCmd else parentConfig.execCmd,
            if (childConfig.inPlace != null) childConfig.inPlace else parentConfig.inPlace,
            if (childConfig.destinationFileSuffix != null) childConfig.destinationFileSuffix else parentConfig.destinationFileSuffix,
        )
    }
}
