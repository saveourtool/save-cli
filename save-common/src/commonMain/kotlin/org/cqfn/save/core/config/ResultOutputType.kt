package org.cqfn.save.core.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Possible data output streams
 */
@Serializable
enum class ResultOutputType {
    @SerialName("file") FILE,
    @SerialName("stderr") STDERR,
    @SerialName("stdout") STDOUT,
    ;
}
