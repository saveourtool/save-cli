package org.cqfn.save.core.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Possible debug mode
 */
@Serializable
enum class DebugType {
    @SerialName("none") NO_DEBUG,
    @SerialName("light") LIGHT_DEBUG,
    @SerialName("hard") HARD_DEBUG,
    ;
}