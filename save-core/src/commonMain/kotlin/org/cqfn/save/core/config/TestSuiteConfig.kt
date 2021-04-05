package org.cqfn.save.core.config

import okio.Path

/**
 * Configuration for a test suite, that is read from test suite configuration file (toml config)
 * @property suiteName name of test suite
 * @property description description of test suite
 * @property location [Path] denoting the location of this file
 * @property parentConfig parent config in the hierarchy of configs, `null` if this config is root.
 */
data class TestSuiteConfig(
    val suiteName: String,
    val description: String,
    val location: Path,
    val parentConfig: TestSuiteConfig?
) {
    /**
     * @return whether this config file is in the root on the hierarchy
     */
    fun isRoot() = parentConfig == null

    /**
     * @return a [Sequence] of parent config files
     */
    fun parentConfigs() = generateSequence(parentConfig) { it.parentConfig }
}

/**
 * @return whether a file denoted by this [Path] is a default save configuration file (save.toml)
 */
fun Path.isDefaultSaveConfig() = name == "save.toml"
