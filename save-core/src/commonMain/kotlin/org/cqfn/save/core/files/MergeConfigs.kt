package org.cqfn.save.core.files

import org.cqfn.save.core.config.TestConfig

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
        mergeCurrentConfigWithParents(testConfig)
        mergeChildConfigsWithCurrent(testConfig)
    }

    /**
     * Merge parent configurations with current
     *
     * @param testConfig - testing SAVE config which should be merged with all previous (parent) configs
     */
    private fun mergeCurrentConfigWithParents(testConfig: TestConfig) {

    }

    /**
     * Prolong current configuration for all child configs
     *
     * @param testConfig - testing SAVE config from which all child SAVE configs file should be prolonged
     */
    private fun mergeChildConfigsWithCurrent(testConfig: TestConfig) {

    }
}