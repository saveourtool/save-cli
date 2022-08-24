package com.saveourtool.save.core.config

/**
 * Configuration for an evaluated tool, that is read from cli
 *
 * @property batchSize it controls how many files execCmd will process at a time
 * @property batchSeparator A separator to join test files to string if the tested tool supports processing of file batches (`batch-size` > 1)
 */
data class EvaluatedToolConfig(
    val batchSize: Int,
    val batchSeparator: String,
)
