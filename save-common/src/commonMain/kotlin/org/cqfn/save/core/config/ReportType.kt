package org.cqfn.save.core.config

import kotlinx.serialization.Serializable

/**
 * Possible types of output formats.
 */
@Serializable
enum class ReportType {
    JSON, PLAIN, TOML, XML
}
