package com.saveourtool.save.core.config

/**
 * Configuration for an evaluated tool, that is read from test suite configuration file (toml config) or cli
 *
 * @property execCmd
 * @property execFlags
 * @property batchSize it controls how many files execCmd will process at a time
 * @property batchSeparator A separator to join test files to string if the tested tool supports processing of file batches (`batch-size` > 1)
 */
data class EvaluatedToolConfig(
    val execCmd: String?,
    val execFlags: String?,
    val batchSize: Int,
    val batchSeparator: String,
)
