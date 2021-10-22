package org.cqfn.save.core.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Possible debug mode
 */
@Serializable
enum class LogType {
    @SerialName("results_only") RESULTS_ONLY,
    @SerialName("warnings") WARN,
    @SerialName("debug") DEBUG,
    @SerialName("all") ALL,
    ;
}
