package com.saveourtool.save.core.config

/**
 * Configuration for an evaluated tool, that is read from test suite configuration file (toml config) or cli
 *
 * @property execCmd
 * @property execFlags
 * @property batchSize
 * @property batchSeparator
 */
data class EvaluatedToolConfig(
    val execCmd: String?,
    val execFlags: String?,
    val batchSize: Int,
    val batchSeparator: String,
)
