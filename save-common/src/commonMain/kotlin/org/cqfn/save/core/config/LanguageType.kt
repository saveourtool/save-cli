package org.cqfn.save.core.config

import kotlinx.serialization.Serializable

/**
 * Available languages
 */
@Suppress("IDENTIFIER_LENGTH")
@Serializable
enum class LanguageType {
    C, CPP, JAVA, KOTLIN, UNDEFINED
}
