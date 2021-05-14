package org.cqfn.save.core.config

import kotlinx.serialization.Serializable

/**
 * Possible data output streams
 */
@Serializable
enum class ResultOutputType {
    FILE, STDERR, STDOUT
}
