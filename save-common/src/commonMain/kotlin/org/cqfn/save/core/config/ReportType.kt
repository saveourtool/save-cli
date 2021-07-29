package org.cqfn.save.core.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Possible types of output formats.
 */
@Serializable
enum class ReportType {
    @SerialName("json") JSON,
    @SerialName("plain") PLAIN,
    @SerialName("test") TEST,
    @SerialName("toml") TOML,
    @SerialName("xml") XML,
    ;
}
