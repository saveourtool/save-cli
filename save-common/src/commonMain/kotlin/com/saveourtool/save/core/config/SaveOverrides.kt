package com.saveourtool.save.core.config

import kotlinx.serialization.Serializable

/**
 * @property execCmd
 * @property execFlags
 * @property batchSize
 * @property batchSeparator
 */
data class SaveOverrides(
    val execCmd: String?,
    val execFlags: String?,
    val batchSize: Int,
    val batchSeparator: String,
) {
    /**
     * @property execCmd
     * @property execFlags
     * @property batchSize
     * @property batchSeparator
     */
    @Serializable
    data class SaveOverridesInterim (
        val execCmd: String? = null,
        val execFlags: String? = null,
        val batchSize: Long? = null,
        val batchSeparator: String? = null
    ) : Interim<SaveOverrides, SaveOverridesInterim> {
        override fun merge(overrides: SaveOverridesInterim) = SaveOverridesInterim(
            overrides.execCmd ?: execCmd,
            overrides.execFlags ?: execFlags,
            overrides.batchSize ?: batchSize,
            overrides.batchSeparator ?: batchSeparator,
        )

        override fun build() = SaveOverrides(
            execCmd = execCmd,
            execFlags = execFlags,
            batchSize = batchSize?.toInt() ?: 1,
            batchSeparator = batchSeparator ?: ", ",
        )
    }
}
