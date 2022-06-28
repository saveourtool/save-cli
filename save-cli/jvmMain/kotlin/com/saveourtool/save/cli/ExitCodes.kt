package com.saveourtool.save.cli

/**
 * @property code numeric value of exit code
 */
enum class ExitCodes(val code: Int) {
    GENERAL_ERROR(1),
    INVALID_CONFIGURATION(2),
    ;
}
