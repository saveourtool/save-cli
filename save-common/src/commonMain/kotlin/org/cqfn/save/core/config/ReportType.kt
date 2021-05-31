package org.cqfn.save.core.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Possible types of output formats.
 */
@Serializable
enum class ReportType {
    @SerialName("JSON") JSON,
    @SerialName("plain") PLAIN,
    @SerialName("TOML") TOML,
    @SerialName("XML") XML,
    ;
}
