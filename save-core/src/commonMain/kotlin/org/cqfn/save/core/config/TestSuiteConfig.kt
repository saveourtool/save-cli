package org.cqfn.save.core.config

/**
 * Configuration for a test suite, that is read from test suite configuration file (toml config)
 * @property suiteName name of test suite
 * @property description dsecription of test suite
 */
data class TestSuiteConfig(
    val suiteName: String,
    val description: String,
)
