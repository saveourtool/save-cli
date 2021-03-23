package org.cqfn.save.core.config

import okio.ExperimentalFileSystem
import okio.Path

/**
 * Configuration properties of save application, retrieved either from peoperties file
 * or from CLI args.
 * @property configPath path to the configuration file
 * @property debug
 * @property quiet
 * @property reportType type of generated report with execution results
 * @property baselinePath path to the file with baseline data
 */
@OptIn(ExperimentalFileSystem::class)
data class SaveConfig(
    val configPath: Path,
    val debug: Boolean,
    val quiet: Boolean,
    val reportType: ReportType,
    val baselinePath: Path?,
)
