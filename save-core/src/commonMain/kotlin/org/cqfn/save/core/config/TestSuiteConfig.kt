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
    val parentConfig: TestSuiteConfig?,
) {
    /**
     * List of child configs in the hierarchy of configs, can be empty if this config is at the very bottom.
     * NB: don't move to constructor in order not to break toString into infinite recursion.
     */
    val childConfigs: MutableList<TestSuiteConfig> = mutableListOf()

    /**
     * @return whether this config file is in the root on the hierarchy
     */
    fun isRoot() = parentConfig == null

    /**
     * @param wihSelf if true, include this config as the first element of the sequence or start with parent config otherwise
     * @return a [Sequence] of parent config files
     */
    fun parentConfigs(wihSelf: Boolean = false) = generateSequence(if (wihSelf) this else parentConfig) { it.parentConfig }
}

/**
 * @return whether a file denoted by this [Path] is a default save configuration file (save.toml)
 */
fun Path.isDefaultSaveConfig() = name == "save.toml"
