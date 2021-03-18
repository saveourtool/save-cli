package org.cqfn.save.core.config

/**
 * Configuration properties of save application, retrieved either from peoperties file
 * or from CLI args.
 */
data class SaveConfig(
    val debug: Boolean,
    val quiet: Boolean,
    val reportType: ReportType,
)
