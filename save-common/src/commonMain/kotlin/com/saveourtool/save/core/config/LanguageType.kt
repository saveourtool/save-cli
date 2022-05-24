package com.saveourtool.save.core.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Available languages
 */
@Suppress("IDENTIFIER_LENGTH")
@Serializable
enum class LanguageType {
    @SerialName("c") C,
    @SerialName("cpp") CPP,
    @SerialName("java") JAVA,
    @SerialName("kotlin") KOTLIN,
    @SerialName("undefined") UNDEFINED,
    ;
}
