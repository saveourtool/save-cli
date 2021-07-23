package org.cqfn.save.cli

/**
 * @property code numeric value of exit code
 */
enum class ExitCodes(val code: Int) {
    GENERAL_ERROR(1),
    INVALID_CONFIGURATION(2),
    ;

    fun foo (line: Int) {
        5 .. line
        5 downTo line
    }
}
