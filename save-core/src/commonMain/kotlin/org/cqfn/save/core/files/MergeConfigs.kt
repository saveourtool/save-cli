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
        logDebug("Start merging configs for ${testConfig.location}")
        val parentConfigs = collectParentConfigs(testConfig)
        mergeConfigList(parentConfigs)
        mergeChildConfigs(testConfig)
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

    private fun mergeChildConfigs(testConfig: TestConfig) {
        for (child in testConfig.childConfigs) {
            mergeChildConfigWithParent(testConfig, child)
            mergeChildConfigs(child)
        }
    }

    // Merge configurations
    private fun mergeConfigList(configList: MutableList<TestConfig>) {
        if (configList.size == 1) {
            return
        }
        val pairs = configList.zipWithNext()

        pairs.forEach { (parent, child) ->
            mergeChildConfigWithParent(parent, child)
        }
    }

    private fun mergeChildConfigWithParent(parent: TestConfig, child: TestConfig) {
        logDebug("Merging ${parent.location} with ${child.location}")
        val parentConfig = parent.pluginConfigs
        val childConfig = child.pluginConfigs
        // Create the list of corresponding configs, if one of them will be null -> list will contain only one element,
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

    @Suppress("AVOID_NULL_CHECKS")
    private fun mergeGeneralConfigs(parentConfig: GeneralConfig, childConfig: GeneralConfig) = GeneralConfig(
        if (childConfig.tags != null) childConfig.tags else parentConfig.tags,
        if (childConfig.description != null) childConfig.description else parentConfig.description,
        if (childConfig.suiteName != null) childConfig.suiteName else parentConfig.suiteName,
        if (childConfig.excludedTests != null) childConfig.excludedTests else parentConfig.excludedTests,
        if (childConfig.includedTests != null) childConfig.includedTests else parentConfig.includedTests,
    )

    @Suppress("AVOID_NULL_CHECKS")
    private fun mergeWarnConfigs(parentConfig: WarnPluginConfig, childConfig: WarnPluginConfig) = WarnPluginConfig(
        if (childConfig.execCmd != null) childConfig.execCmd else parentConfig.execCmd,
        if (childConfig.warningsInputPattern != null) childConfig.warningsInputPattern else parentConfig.warningsInputPattern,
        if (childConfig.warningsOutputPattern != null) childConfig.warningsOutputPattern else parentConfig.warningsOutputPattern,
        if (childConfig.warningTextHasLine != null) childConfig.warningTextHasLine else parentConfig.warningTextHasLine,
        if (childConfig.warningTextHasColumn != null) childConfig.warningTextHasColumn else parentConfig.warningTextHasColumn,
        if (childConfig.lineCaptureGroup != null) childConfig.lineCaptureGroup else parentConfig.lineCaptureGroup,
        if (childConfig.columnCaptureGroup != null) childConfig.columnCaptureGroup else parentConfig.columnCaptureGroup,
        if (childConfig.messageCaptureGroup != null) childConfig.messageCaptureGroup else parentConfig.messageCaptureGroup,
    )

    @Suppress("AVOID_NULL_CHECKS")
    private fun mergeFixConfigs(parentConfig: FixPluginConfig, childConfig: FixPluginConfig) = FixPluginConfig(
        if (childConfig.execCmd != null) childConfig.execCmd else parentConfig.execCmd,
        if (childConfig.destinationFileSuffix != null) childConfig.destinationFileSuffix else parentConfig.destinationFileSuffix,
    )
}
