package com.saveourtool.save.core.config

import kotlinx.serialization.Serializable

data class SaveOverrides(
    val execCmd: String?,
    val execFlags: String?,
    val batchSize: Int,
    val batchSeparator: String,
) {
    constructor(nullable: Nullable) : this(
        execCmd = nullable.execCmd,
        execFlags = nullable.execFlags,
        batchSize = nullable.batchSize ?: 1,
        batchSeparator = nullable.batchSeparator ?: ", ",
    )

    @Serializable
    data class Nullable(
        val execCmd: String? = null,
        val execFlags: String? = null,
        val batchSize: Int? = null,
        val batchSeparator: String? = null
    ) {
        fun merge(override: Nullable): Nullable {
            return Nullable(
                override.execCmd ?: execCmd,
                override.execFlags ?: execFlags,
                override.batchSize ?: batchSize,
                override.batchSeparator ?: batchSeparator,
            )
        }
    }
}
