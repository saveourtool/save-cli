package org.cqfn.save.core.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Possible debug mode
 */
@Serializable
enum class DebugType {
    @SerialName("none") NONE,
    @SerialName("light") LIGHT,
    @SerialName("hard") HARD,
    ;
}