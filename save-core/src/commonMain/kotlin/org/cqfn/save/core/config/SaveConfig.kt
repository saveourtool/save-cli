package org.cqfn.save.core.config

import okio.ExperimentalFileSystem
import okio.Path

/**
 * Configuration properties of save application, retrieved either from peoperties file
 * or from CLI args.
 * @property configPath
 * @property debug
 * @property quiet
 * @property reportType
 */
@OptIn(ExperimentalFileSystem::class)
data class SaveConfig(
    val configPath: Path,
    val debug: Boolean,
    val quiet: Boolean,
    val reportType: ReportType,
)
