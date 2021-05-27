package org.cqfn.save.core.files

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.plugin.GeneralConfig
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
        val parentConfigs = testConfig.parentConfigs(withSelf = true).toList().asReversed()
        mergeConfigList(parentConfigs)
        mergeChildConfigs(testConfig)
    }

    // Merge list of configs pairwise
    private fun mergeConfigList(configList: List<TestConfig>) {
        if (configList.size == 1) {
            return
        }
        val pairs = configList.zipWithNext()

        pairs.forEach { (parent, child) ->
            mergeChildConfigWithParent(parent, child)
        }
    }

    // Merge child configs recursively
    private fun mergeChildConfigs(testConfig: TestConfig) {
        for (child in testConfig.childConfigs) {
            mergeChildConfigWithParent(testConfig, child)
            mergeChildConfigs(child)
        }
    }

    private fun mergeChildConfigWithParent(parent: TestConfig, child: TestConfig) {
        logDebug("Merging ${parent.location} with ${child.location}")
        val parentConfig = parent.pluginConfigs
        val childConfig = child.pluginConfigs
        // Create the list of corresponding configs, if only one of them will be null -> list will contain another element,
        // which we apply as final config.
        // If both of them will be null, then we should do nothing and return null.
        // Otherwise we will merge configs
        val generalConfigs = listOfNotNull(
            parentConfig.filterIsInstance<GeneralConfig>().firstOrNull(),
            childConfig.filterIsInstance<GeneralConfig>().firstOrNull()
        )
        val newGeneralConfig: GeneralConfig? = if (generalConfigs.size != 2) generalConfigs.firstOrNull() else generalConfigs.last().mergePluginConfig(generalConfigs.first())

        val warnConfigs = listOfNotNull(
            parentConfig.filterIsInstance<WarnPluginConfig>().firstOrNull(),
            childConfig.filterIsInstance<WarnPluginConfig>().firstOrNull()
        )
        val newWarnConfig: WarnPluginConfig? = if (warnConfigs.size != 2) warnConfigs.firstOrNull() else warnConfigs.last().mergePluginConfig(warnConfigs.first())

        val fixConfigs = listOfNotNull(
            parentConfig.filterIsInstance<FixPluginConfig>().firstOrNull(),
            childConfig.filterIsInstance<FixPluginConfig>().firstOrNull()
        )
        val newFixConfig: FixPluginConfig? = if (fixConfigs.size != 2) fixConfigs.firstOrNull() else fixConfigs.last().mergePluginConfig(fixConfigs.first())

        // Now we update child config in place
        childConfig.clear()
        val result = listOfNotNull(newGeneralConfig, newWarnConfig, newFixConfig)
        result.forEach { childConfig.add(it) }
    }
}
