package com.saveourtool.save.core.utils

/**
 * An [Exception] that can be thrown in case of failed to parse sarif
 */
class SarifParsingException(message: String, cause: Throwable?) : Exception(message, cause)
