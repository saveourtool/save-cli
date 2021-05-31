package org.cqfn.save.core.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Available languages
 */
@Suppress("IDENTIFIER_LENGTH")
@Serializable
enum class LanguageType {
    @SerialName("C") C,
    @SerialName("CPP") CPP,
    @SerialName("Java") JAVA,
    @SerialName("Kotlin") KOTLIN,
    @SerialName("undefined") UNDEFINED,
    ;
}
